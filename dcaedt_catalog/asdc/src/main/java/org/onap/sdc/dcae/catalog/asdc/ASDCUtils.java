package org.onap.sdc.dcae.catalog.asdc;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.catalog.commons.Actions;
import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.commons.Futures;
import org.onap.sdc.dcae.catalog.commons.Recycler;
import org.onap.sdc.dcae.checker.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.io.*;
import java.net.URI;
import java.util.*;
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

    public CloneAssetArtifactsAction cloneAssetArtifacts(ASDC.AssetType theAssetType, UUID theSourceId, UUID theTargetId) {
        return new CloneAssetArtifactsAction(this.asdc, theAssetType, theSourceId, theTargetId);
    }

    public static class CloneAssetArtifactsAction extends ASDC.ASDCAction<CloneAssetArtifactsAction, List<JSONObject>> {

        private ASDC.AssetType assetType;
        private UUID sourceId, targetId;

        CloneAssetArtifactsAction(ASDC theASDC, ASDC.AssetType theAssetType, UUID theSourceId, UUID theTargetId) {
            theASDC.super(new JSONObject());
            this.assetType = theAssetType;
            this.sourceId = theSourceId;
            this.targetId = theTargetId;
        }

        protected CloneAssetArtifactsAction self() {
            return this;
        }

        public CloneAssetArtifactsAction withLabel(String theLabel) {
            return with("artifactLabel", theLabel);
        }

        protected String[] mandatoryInfoEntries() {
            return new String[] {};
        }

        public Future<List<JSONObject>> execute() {
            checkMandatory();

            final Actions.Sequence<JSONObject> sequencer = new Actions.Sequence<JSONObject>();

            new Actions.Sequence().add(super.asdc().getAssetArchiveAction(this.assetType, this.sourceId)).add(super.asdc().getAssetAction(this.assetType, this.sourceId, JSONObject.class)).execute().setHandler(assetFuture -> {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "*** {}", assetFuture.result());
                processArtifacts((List) assetFuture.result(), (JSONObject theInfo, byte[] theData) -> {
                    theInfo.remove("artifactChecksum");
                    theInfo.remove("artifactUUID");
                    theInfo.remove("artifactVersion");
                    theInfo.remove(ARTIFACT_URL);
                    theInfo.put("description", theInfo.remove("artifactDescription"));
                    theInfo.put("payloadData", Base64Utils.encodeToString(theData));
                    return theInfo;
                }, null).forEach(artifactInfo -> sequencer.add(super.asdc().createAssetArtifact(this.assetType, this.targetId).withInfo(ASDC.merge(artifactInfo, this.info)).withOperator(this.operatorId)));
                sequencer.execute();
            });

            return sequencer.future();
        }
    } //the Action class

    /* */
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

    /**
     * Recycle a cdump, fetch all relevant ASDC artifacts, interact with Shu's toscalib service in order to generate
     * a blueprint. No 'Action' object here as there is nothig to set up.
     */
    public Future<Future<String>> buildBlueprint(Reader theCdump) {

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
        List uuids = (List) StreamSupport.stream(Spliterators.spliteratorUnknownSize(jxroot.iterate("topology_template/node_templates/*/description"), 16), false).distinct().filter(desc -> desc != null)
                //the desc contains the full URI and the resource uuid is the 5th path element
                .map(desc -> desc.toString().split("/")[5]).collect(Collectors.toList());

        //prepare fetching all archives/resource details
        final Futures.Accumulator accumulator = new Futures.Accumulator();
        uuids.forEach(uuid -> {
            UUID rid = UUID.fromString((String) uuid);
            accumulator.add(this.asdc.getAssetArchive(ASDC.AssetType.resource, rid));
            accumulator.add(this.asdc.getAsset(ASDC.AssetType.resource, rid, JSONObject.class));
        });

        final byte[] templateData = recycler.toString(template).getBytes(/*"UTF-8"*/);
        //retrieve all resource archive+details, prepare blueprint service request and send its request
        return Futures.advance(accumulator.accumulate(), (List theArchives) -> {
            Blueprinter.BlueprintAction action = blueprint.generateBlueprint();
            processArtifacts(theArchives, (JSONObject theInfo, byte[] theData) -> new JSONObject().put(theInfo.getString(ARTIFACT_NAME).split("\\.")[0], Base64Utils.encodeToString(theData)),
                    (Stream<JSONObject> theAssetArtifacts) -> theAssetArtifacts.reduce(new JSONObject(), ASDC::merge)).forEach(artifactInfo -> action.withModelInfo(artifactInfo));

            return action.withTemplateData(templateData).execute();
        });
    }

    public Future<Future<String>> buildBlueprintViaToscaLab(Reader theCdump) {
        return processCdump(theCdump, (theTemplate, theArchives) -> {
            Blueprinter.BlueprintAction action = blueprint.generateBlueprint();
            processArtifacts(theArchives, (JSONObject theInfo, byte[] theData) -> new JSONObject().put(theInfo.getString(ARTIFACT_NAME).split("\\.")[0], Base64Utils.encodeToString(theData)),
                    (Stream<JSONObject> theAssetArtifacts) -> theAssetArtifacts.reduce(new JSONObject(), ASDC::merge)).forEach(artifactInfo -> action.withModelInfo(artifactInfo));

            return action.withTemplateData(Recycler.toString(theTemplate).getBytes()).execute();

        });
    }

    private static class Tracker implements TargetLocator {

        private enum Position {
            SCHEMA, TEMPLATE, TRANSLATE;
        }

        private static final int POSITIONS = Position.values().length;

        private List<Target> tgts = new ArrayList<Target>(3);

        Tracker() {
            clear();
        }

        public boolean addSearchPath(URI theURI) {
            return false;
        }

        public boolean addSearchPath(String thePath) {
            return false;
        }

        public Iterable<URI> searchPaths() {
            return Collections.emptyList();
        }

        int position(String... theKeys) {
            for (String key : theKeys) {
                if ("schema".equals(key)) {
                    return Position.SCHEMA.ordinal();
                }
                if ("template".equals(key)) {
                    return Position.TEMPLATE.ordinal();
                }
                if ("translate".equals(key)) {
                    return Position.TRANSLATE.ordinal();
                }
            }
            return -1;
        }

        public Target resolve(String theName) {
            for (Target tgt : tgts) {
                if (tgt != null && tgt.getName().equals(theName)) {
                    return tgt;
                }
            }
            return null;
        }

        void track(JSONObject theInfo, final byte[] theData) {
            String uri = theInfo.getString(ARTIFACT_URL).split("/")[5];
            String name = theInfo.getString(ARTIFACT_NAME), desc = theInfo.getString("artifactDescription"), label = theInfo.getString("artifactLabel");
            int pos = position(desc, label);

            debugLogger.log(LogLevel.DEBUG, ASDCUtils.class.getName(), "Tracking {} at {}, {}", name, pos, theInfo.optString(ARTIFACT_URL));

            if (pos > -1) {
                tgts.set(pos, new Target(name, URI.create("asdc:" + uri + "/" + name)) {
                    @Override
                    public Reader open(){
                        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(theData)));
                    }
                });
            }
        }

        boolean hasSchema() {
            return tgts.get(Position.SCHEMA.ordinal()) != null;
        }

        public Target schema() {
            return tgts.get(Position.SCHEMA.ordinal());
        }

        boolean hasTemplate() {
            return tgts.get(Position.TEMPLATE.ordinal()) != null;
        }

        public Target template() {
            return tgts.get(Position.TEMPLATE.ordinal());
        }

        boolean hasTranslation() {
            return tgts.get(Position.TRANSLATE.ordinal()) != null;
        }

        public Target translation() {
            return tgts.get(Position.TRANSLATE.ordinal());
        }

        public void clear() {
            if (tgts.isEmpty()) {
                for (int i = 0; i < POSITIONS; i++) {
                    tgts.add(null);
                }
            } else {
                Collections.fill(tgts, null);
            }
        }
    }

    private Checker buildChecker() {
        try {
            return new Checker();
        } catch (CheckerException cx) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "CheckerException while creating Checker {}", cx);
            return null;
        }
    }

    public Future<Catalog> buildCatalog(Reader theCdump) {

        //
        //the purpose of the tracking is to be able to resolve import references within the 'space' of an
        //asset's artifacts
        //processing order is important too so we 'order the targets: schema, template, translation
        //
        final Tracker tracker = new Tracker();
        final Catalog catalog = Checker.buildCatalog();

        return processCdump(theCdump, (theTemplate, theArchives) -> {

            final Checker checker = buildChecker();
            if (checker == null) {
                return null;
            }
            checker.setTargetLocator(tracker);

            processArtifacts(theArchives, (JSONObject theInfo, byte[] theData) -> {
                        tracker.track(theInfo, theData);
                        return (Catalog) null;
                    },
                    // aggregation: this is where the actual processing takes place now that
                    // we have all the targets
                    (Stream<Catalog> theAssetArtifacts) -> checkAndGetCatalog(tracker, catalog, checker));

            Target cdump = new Target("cdump", URI.create("asdc:cdump"));
            cdump.setTarget(theTemplate);

            validateCatalog(catalog, checker, cdump);

            return catalog;
        });
    }

    private Catalog checkAndGetCatalog(Tracker tracker, Catalog catalog, Checker checker) {
        //the stream is full of nulls, ignore it, work with the tracker

        try {
            if (tracker.hasSchema()) {
                checker.check(tracker.schema(), catalog);
            }
            if (tracker.hasTemplate()) {
                checker.check(tracker.template(), catalog);
            }
            if (tracker.hasTranslation()) {
                checker.check(tracker.translation(), catalog);
            }
        } catch (CheckerException cx) {
            //got to do better than this
            errLogger.log(LogLevel.ERROR, ASDC.class.getName(),"CheckerException while checking catalog:{}", cx);
        } finally {
            tracker.clear();
        }
        return checker.catalog();
    }

    private void validateCatalog(Catalog catalog, Checker checker, Target cdump) {
        try {
            checker.validate(cdump, catalog);
        } catch (CheckerException cx) {
            errLogger.log(LogLevel.ERROR, ASDC.class.getName(),"CheckerException while building catalog:{}", cx);
        }
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
