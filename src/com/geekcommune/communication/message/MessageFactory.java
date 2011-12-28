package com.geekcommune.communication.message;


/**
 * Used to create messages when reading from the stream.
 * @author bobbym
 *
 */
public interface MessageFactory {

    Message makeMessage();

}
