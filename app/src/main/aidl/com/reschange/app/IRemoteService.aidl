// IRemoteService.aidl
package com.reschange.app;

interface IRemoteService {
    int execute(String command);
    String read(String command);
    void destroy();
}
