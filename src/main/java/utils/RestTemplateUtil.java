package utils;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class RestTemplateUtil {

    public static RestTemplate getRestTemplate(int timeout) {
        return new RestTemplate(getClientHttpRequestFactory(timeout));
    }

    private static ClientHttpRequestFactory getClientHttpRequestFactory(int timeout) {
        HttpComponentsClientHttpRequestFactory commonsClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        commonsClientHttpRequestFactory.setReadTimeout(timeout);
        return commonsClientHttpRequestFactory;
    }
}
