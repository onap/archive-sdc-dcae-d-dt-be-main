
import json.response.ItemsResponse.Item;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import tools.EntitiesRetriever;

import java.util.List;
import java.util.Map;


public class EntitiesRetrieverTest extends BaseTest {

    @InjectMocks
    EntitiesRetriever entitiesRetriever;

    @Before
    @Override
    public void setup() {
        super.setup();
    }

    @Test
    public void getElementsByFolder_returns2Items() {
        Map<String, List<Item>> result = entitiesRetriever.getElementsByFolder();
        Assert.assertTrue(result.size() == 2);
    }
}
