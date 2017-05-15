// CountAIDL.aidl
package com.yimeng.servicetest.service;
import com.yimeng.servicetest.service.OnChangeListenerAIDL;

// Declare any non-default types here with import statements

interface CountAIDL {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
            void add();
            void addListener(OnChangeListenerAIDL listener);
            void removeListener(OnChangeListenerAIDL listener);
}
