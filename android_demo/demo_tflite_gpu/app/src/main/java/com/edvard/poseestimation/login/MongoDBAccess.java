package com.edvard.poseestimation.login;

import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.mongodb.lang.NonNull;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateOptions;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;

import org.bson.Document;
import javax.security.auth.login.LoginException;

public class MongoDBAccess {

    private StitchAppClient client;
    private RemoteMongoCollection<Document> coll;

    public MongoDBAccess() {
        client = Stitch.initializeDefaultAppClient("stitchtest-mhonv");

        final RemoteMongoClient mongoClient =
                client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");

        coll = mongoClient.getDatabase("androidDB").getCollection("usertTest");
    }

    public void validateUserPassword(String username, String password) {
        Document filterDoc = new Document("username", username);
        Task<Document> result = coll.findOne(filterDoc);

        client.getAuth().loginWithCredential(new AnonymousCredential()).continueWithTask(
                task -> {
                    if (!task.isSuccessful()) {
                        Log.e("STITCH", "Login failed!");
                        throw task.getException();
                    }
                    return coll.findOne(filterDoc);
                }
        ).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Document item = task.getResult();
            } else {
                try {
                    throw new LoginException("failed to find documents with: " + task.getException());
                } catch (LoginException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
