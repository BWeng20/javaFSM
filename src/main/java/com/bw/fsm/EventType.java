package com.bw.fsm;

/**
 * Possible event types.
 * @see Event#etype
 */
public enum EventType {

    /**
     * for events raised by the platform itself, such as error events
     */
    platform,

    /**
     * for events raised by &lt;raise> and &lt;send> with target '_internal'
     */
    internal,

    /**
     * <b>default</b> - for all other events
     */
    external,
}

