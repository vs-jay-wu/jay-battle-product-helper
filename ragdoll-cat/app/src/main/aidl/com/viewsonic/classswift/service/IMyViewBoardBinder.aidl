// IMyViewBoardBinder.aidl
package com.viewsonic.classswift.service;

import com.viewsonic.classswift.service.IMyViewBoardCallback;

interface IMyViewBoardBinder {
    void send(in String jsonMsg);
    void registerCallback(in IMyViewBoardCallback cb);
    void unregisterCallback(in IMyViewBoardCallback cb);
}
