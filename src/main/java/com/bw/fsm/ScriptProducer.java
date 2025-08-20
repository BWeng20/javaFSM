package com.bw.fsm;

import com.bw.fsm.datamodel.Datamodel;

/**
 * Interface to produce Datamodel specific script from Data.
 *
 * @see Datamodel#createScriptProducer()
 */
public interface ScriptProducer {

    /**
     * Starts a new array.
     */
    void startArray();

    /**
     * Starts a new element in an array.
     */
    void startArrayMember();

    /**
     * Ends the current array member.
     */
    void endArrayMember();

    /**
     * Ends the current array.
     */
    void endArray();

    /**
     * Starts a new map (used also for objects).
     */
    void startMap();

    /**
     * Starts with a new member of the map.
     */
    void startMember(String name);

    /**
     * Ends the current member.
     */
    void endMember();

    /**
     * Adds a member to the current map.
     *
     * @param name The name of the entry
     * @param data The data.
     */
    default void addStringMember(String name, String data) {
        startMember(name);
        addToken(asStringValue(data));
        endMember();
    }

    /**
     * Adds a member to the current map.
     *
     * @param name The name of the entry
     * @param data The data.
     */
    default void addDataMember(String name, Data data) {
        startMember(name);
        if (data == null)
            addNull();
        else
            data.as_script(this);
        endMember();
    }

    /**
     * Ends the current map.
     */
    void endMap();

    /**
     * Adds a null value.
     */
    void addNull();

    /**
     * Adds a numeric value.
     */
    void addValue(Number value);

    /**
     * Adds some raw text.
     */
    void addToken(String value);

    /**
     * Finish the current script, returns the content and reset the internal buffers.
     * After this, the Producer is ready to start a new script.
     */
    String finish();

    /**
     * Converts the value into a language specific string value (quoted and escaped if needed).
     */
    String asStringValue(String value);

}
