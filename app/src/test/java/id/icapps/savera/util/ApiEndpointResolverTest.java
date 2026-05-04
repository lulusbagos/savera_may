package id.icapps.savera.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.List;

import id.icapps.savera.LocalStorage;
import id.icapps.savera.test.TestBase;

public class ApiEndpointResolverTest extends TestBase {
    private LocalStorage localStorage;

    @Before
    public void setUpResolverState() {
        localStorage = new LocalStorage(getContext());
        localStorage.setApiPublicUrl("https://savera-api.ungguldinamika.com/api");
        localStorage.setApiLocalUrl("http://10.10.10.25:2026/api");
        localStorage.setApiPreferredRoute("public");
        localStorage.setApiActiveBaseUrl("");
    }

    @Test
    public void defaultFlowTriesPublicBeforeLocal() throws Exception {
        List<URL> candidates = ApiEndpointResolver.candidateUrls(getContext(), "https://savera-api.ungguldinamika.com/api/summary");

        assertEquals("https://savera-api.ungguldinamika.com/api/summary", candidates.get(0).toString());
        assertEquals("http://savera-api.ungguldinamika.com/api/summary", candidates.get(1).toString());
        assertEquals("http://10.10.10.25:2026/api/summary", candidates.get(2).toString());
    }

    @Test
    public void syncedLocalPreferredFlowMovesLocalToFront() throws Exception {
        localStorage.syncApiConfig(
                "https://savera-api.ungguldinamika.com/api",
                "http://10.10.10.25:2026/api",
                "local",
                "http://10.10.10.25:2026/api"
        );

        List<URL> candidates = ApiEndpointResolver.candidateUrls(getContext(), "https://savera-api.ungguldinamika.com/api/detail");

        assertEquals("http://10.10.10.25:2026/api/detail", candidates.get(0).toString());
        assertEquals("https://savera-api.ungguldinamika.com/api/detail", candidates.get(1).toString());
    }
}
