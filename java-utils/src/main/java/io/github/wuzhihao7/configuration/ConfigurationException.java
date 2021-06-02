package io.github.wuzhihao7.configuration;

public class ConfigurationException extends RuntimeException{
    public ConfigurationException(String message){
        super(message);
    }

    public ConfigurationException(Throwable cause){
        super(cause);
    }

    public ConfigurationException(String message, Throwable cause){
        super(message, cause);
    }
}
