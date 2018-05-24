package org.onap.sdc.dcae.catalog.asdc;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.dcae.catalog.commons.Actions;
import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.commons.Futures;
import org.onap.sdc.dcae.catalog.commons.Recycler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@Component("asdcutils")
@Scope("singleton")
@ConfigurationProperties(prefix="asdcutils")
public class ASDCUtils {

    private static final String ARTIFACT_URL = "artifactURL";
    private static final String ARTIFACT_NAME = "artifactName";
    private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
    private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

    @Autowired
    private ASDC asdc;

    @Autowired
    private Blueprinter blueprint;

    public ASDCUtils() {
        // Making sonar happy
    }

    public ASDCUtils(URI theASDCURI) {
        this(theASDCURI, null);
    }

    public ASDCUtils(URI theASDCURI, URI theBlueprinterURI) {
        this.asdc = new ASDC();
        this.asdc.setUri(theASDCURI);
        if (theBlueprinterURI != null) {
            this.blueprint = new Blueprinter();
            this.blueprint.setUri(theBlueprinterURI);
        }
    }

    public ASDCUtils(ASDC theASDC) {
        this(theASDC, null);
    }

    public ASDCUtils(ASDC theASDC, Blueprinter theBlueprinter) {
        this.asdc = theASDC;
        this.blueprint = theBlueprinter;
    }


    private static JSONObject lookupArtifactInfo(JSONArray theArtifacts, String theName) {

        for (int i = 0; theArtifacts != null && i < theArtifacts.length(); i++) {
            JSONObject artifactInfo = theArtifacts.getJSONObject(i);
            if (theName.equals(artifactInfo.getString(ARTIFACT_NAME))) {
                debugLogger.log(LogLevel.DEBUG, ASDCUtils.class.getName(), "Found artifact info {}", artifactInfo);
                return artifactInfo;
            }
        }

        return null;
    }

    private static byte[] extractArtifactData(InputStream theEntryStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buff = new byte[4096];
            int cnt = 0;
            while ((cnt = theEntryStream.read(buff)) != -1) {
                baos.write(buff, 0, cnt);
            }
        } finally {
            baos.close();
        }
        return baos.toByteArray();
    }


    public Future<Future<String>> buildBlueprintViaToscaLab(Reader theCdump) {
        return processCdump(theCdump, (theTemplate, theArchives) -> {
            Blueprinter.BlueprintAction action = blueprint.generateBlueprint();
            processArtifacts(theArchives, (JSONObject theInfo, byte[] theData) -> new JSONObject().put(theInfo.getString(ARTIFACT_NAME).split("\\.")[0], Base64Utils.encodeToString(theData)),
                    (Stream<JSONObject> theAssetArtifacts) -> theAssetArtifacts.reduce(new JSONObject(), ASDC::merge)).forEach(artifactInfo -> action.withModelInfo(artifactInfo));

            return action.withTemplateData(Recycler.toString(theTemplate).getBytes()).execute();

        });
    }

    /* The common process of recycling, retrieving all related artifacts and then doing 'something' */
    private <T> Future<T> processCdump(Reader theCdump, BiFunction<Object, List, T> theProcessor) {

        final Recycler recycler = new Recycler();
        Object template = null;
        try {
            template = recycler.recycle(theCdump);

        } catch (Exception x) {
            return Futures.failedFuture(x);
        }

        JXPathContext jxroot = JXPathContext.newContext(template);
        jxroot.setLenient(true);

        //based on the output of ASDCCatalog the node description will contain the UUID of the resource declaring it
        //the desc contains the full URI and the resource uuid is the 5th path element
        List uuids = (List) StreamSupport.stream(Spliterators.spliteratorUnknownSize(jxroot.iterate("topology_template/node_templates/*/description"), 16), false).distinct().filter(desc -> desc != null)
                .map(desc -> desc.toString().split("/")[5]).collect(Collectors.toList());

        //serialized fetch version
        final Actions.Sequence sequencer = new Actions.Sequence();
        uuids.stream().forEach(uuid -> {
            UUID rid = UUID.fromString((String) uuid);
            sequencer.add(this.asdc.getAssetArchiveAction(ASDC.AssetType.resource, rid));
            sequencer.add(this.asdc.getAssetAction(ASDC.AssetType.resource, rid, JSONObject.class));
        });

        final Object tmpl = template;
        return Futures.advance(sequencer.execute(), (List theArchives) -> theProcessor.apply(tmpl, theArchives));
    }

    private static <T> Stream<T> processArtifacts(List theArtifactData, BiFunction<JSONObject, byte[], T> theProcessor, Function<Stream<T>, T> theAggregator) {

        Stream.Builder<T> assetBuilder = Stream.builder();

        for (int i = 0; i < theArtifactData.size(); i = i + 2) { //cute old style loop

            JSONObject assetInfo = (JSONObject) theArtifactData.get(i + 1);
            byte[] assetData = (byte[]) theArtifactData.get(i + 0);

            JSONArray artifacts = assetInfo.optJSONArray("artifacts");

            Stream.Builder<T> artifactBuilder = Stream.builder();

            try (ZipInputStream zipper = new ZipInputStream(new ByteArrayInputStream(assetData))){
                //we process the artifacts in the order they are stored in the archive .. fugly
                processZipArtifacts(theProcessor, artifacts, artifactBuilder, zipper);
            } catch (IOException iox) {
                errLogger.log(LogLevel.ERROR, ASDC.class.getName(), "IOException: {}", iox);
                return null;
            }

            if (theAggregator != null) {
                assetBuilder.add(theAggregator.apply(artifactBuilder.build()));
            } else {
                artifactBuilder.build().forEach(entry -> assetBuilder.add(entry));
            }
        }

        return assetBuilder.build();
    }

    private static <T> void processZipArtifacts(BiFunction<JSONObject, byte[], T> theProcessor, JSONArray artifacts, Stream.Builder<T> artifactBuilder, ZipInputStream zipper) throws IOException {
        for (ZipEntry zipped = zipper.getNextEntry(); zipped != null; zipped = zipper.getNextEntry()) {
            JSONObject artifactInfo = lookupArtifactInfo(artifacts, StringUtils.substringAfterLast(zipped.getName(), "/"));
            if (artifactInfo != null) {
                artifactBuilder.add(theProcessor.apply(artifactInfo, extractArtifactData(zipper)));
            }
            zipper.closeEntry();
        }
    }
}
