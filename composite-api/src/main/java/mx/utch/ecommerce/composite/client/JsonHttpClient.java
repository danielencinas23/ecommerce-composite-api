package mx.utch.ecommerce.composite.client;

import java.net.http.HttpTimeoutException;

import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import mx.utch.ecommerce.composite.config.ServicesProperties;

/** Hace peticiones GET que devuelven JSON y convierte cualquier falla en un {@link ServiceException}. */
@Component
public class JsonHttpClient {

    private final RestClient restClient;
    private final ServicesProperties properties;

    public JsonHttpClient(RestClient restClient, ServicesProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public <T> T get(String service, String url, Class<T> responseType, Object... uriVariables) {
        try {
            T body = restClient.get()
                    .uri(url, uriVariables)
                    .retrieve()
                    .body(responseType);
            if (body == null) {
                throw new ServiceException(service, "respuesta vacía");
            }
            return body;
        } catch (RestClientResponseException e) {
            // El servicio respondió, pero con un código de error (4xx o 5xx).
            int status = e.getStatusCode().value();
            throw new ServiceException(service, "respondió HTTP " + status, status, e);
        } catch (ResourceAccessException e) {
            // No hubo respuesta: servicio apagado, error de red o timeout.
            String reason = isTimeout(e)
                    ? "sin respuesta tras " + properties.timeout().toMillis() + " ms (timeout)"
                    : "servicio no disponible";
            throw new ServiceException(service, reason, null, e);
        } catch (RestClientException e) {
            throw new ServiceException(service, "respuesta JSON inválida", null, e);
        }
    }

    private static boolean isTimeout(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof HttpTimeoutException || t instanceof java.net.SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
