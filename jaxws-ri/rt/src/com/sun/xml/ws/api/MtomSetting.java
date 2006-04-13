package com.sun.xml.ws.api;

/**
 * Provides abstraction over MTOM setting, tells whether mtom is enabled and also tells if the setting is default.
 *
 * @author Vivek Pandey
 */
public interface MtomSetting {
    /**
     * Returns true if this binding implies using MTOM.
     * <p/>
     * Note that MTOM can be enabled/disabled at runtime through
     * {@link com.sun.xml.ws.api.WSBinding}, so this value merely controls how things
     * are configured by default.
     */
    boolean isEnabled();

    /**
     * Tells if the Mtom setting is the default or its set explicitly by DD or annotation.
     *
     * @return true tells the mtom setting is explicitly set by DD/annotation and false means its default setting.
     */
    boolean isDefault();
}
