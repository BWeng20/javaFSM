package com.bw.fsm;

public enum EventType {

    /// for events raised by the platform itself, such as error events
    platform,

    /// for events raised by \<raise\> and \<send\> with target '_internal'
    internal,

    // default
    /// for all other events
    external,
}

