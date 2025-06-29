package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.Lambda;
import com.smirnoal.lambda.LambdaApplication;

public class SnapStartRuntimeHooks {

    public SnapStartRuntimeHooks()
    {
        Lambda.SnapStart.registerBeforeSnapshotHook(this::beforeSnapshotMethod);
        Lambda.SnapStart.registerBeforeSnapshotHook(SnapStartRuntimeHooks::beforeSnapshotStaticMethod);
        Lambda.SnapStart.registerAfterRestoreHook(this::afterRestoreMethod);
    }

    void beforeSnapshotMethod() {

    }

    static void beforeSnapshotStaticMethod() {

    }

    void afterRestoreMethod() {

    }

    void snapStartHandler() {

    }


    public static void main(String[] args) {
        SnapStartRuntimeHooks myHandler = new SnapStartRuntimeHooks();

        LambdaApplication app = new LambdaApplication();
        app.run(myHandler::snapStartHandler);
    }
}
