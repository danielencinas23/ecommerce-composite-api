package mx.utch.ecommerce.composite.client;

/** Falla al consultar un servicio: indica cuál servicio falló, por qué y el código HTTP (si hubo respuesta). */
public class ServiceException extends RuntimeException {

    private final String service;
    private final Integer status;

    public ServiceException(String service, String message) {
        this(service, message, null, null);
    }

    public ServiceException(String service, String message, Integer status, Throwable cause) {
        super(message, cause);
        this.service = service;
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public Integer getStatus() {
        return status;
    }
}
