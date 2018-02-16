package com.vaadin.model;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.vaadin.server.VaadinSession;

import java.io.IOException;

/**
 * inits the GoogleAuthorizationCodeFlow, creates the credentials, handles the return codes for the VaadinRequests
 * and stores the current user in the database
 *
 * Requested Scope: https://www.googleapis.com/auth/fitness.activity.read
 */
public class AuthRequest extends ApiRequest{

    /* Authorization Flow */
    private GoogleAuthorizationCodeFlow flow;

    /* Standard Construtor, creates Authentification Flow */
    public AuthRequest() {
        this.flow = initFlow();
    }

    /**
     * init the google authorization code flow
     * @return GoogleAuthorizationCodeFlow
     */
    private GoogleAuthorizationCodeFlow initFlow() {
        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow = null;
        try {
            flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT,
                    JSON_FACTORY,
                    loadClientSecrets(JSON_FACTORY),
                    SCOPES
            ).setAccessType("offline").setApprovalPrompt("force").build();
        } catch (IOException e) {
            System.out.println("Cannot read JSON with Client Secrets");
            e.printStackTrace();
        }

        return flow;
    }

    /**
     * @return REDIRECT_URI for the newAuthorizationUrl
     */
    public String getAuthURI() {
        return flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
    }

    /**
     * creates the credential token
     * @param code: the return code obtained from the VaadinRequest
     * @return the GoogleAuthorizationCodeFlow holding the credential
     * @throws IOException:
     */
    private Credential createCredential(String code) throws IOException {
        GoogleTokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
        System.out.println("Token Response");
        System.out.println(tokenResponse.getIdToken());
        return flow.createAndStoreCredential(tokenResponse, "id1");
    }

    /**
     * handles the return code from the VaadrinRequest by creating the credentials for the vaadin session and storing
     * them in the database
     * @param code: the return code obtained from the VaadinRequest
     */
    void handleReturnCode(String code){
        System.out.println("Code obtained: ");
        System.out.println(code);
        try {
            VaadinSession.getCurrent().setAttribute("sessionCredential", createCredential(code));
            myCredential = (Credential)VaadinSession.getCurrent().getAttribute("sessionCredential");

            // STORE CREDENTIAL IN MONGODB
            updateUserData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * stores the user data in the database and updates the userID for the vaadin session
     * @throws IOException:
     */
    private void updateUserData() throws IOException {
        Oauth2 oauth2 = new Oauth2.Builder(HTTP_TRANSPORT, JSON_FACTORY, myCredential)
                .setApplicationName("TrackFit").build();
        Userinfoplus userInfo = oauth2.userinfo().get().execute();
        System.out.println("User: ");
        System.out.println(userInfo.toPrettyString());

        DbConnector dbConnect = new DbConnector(userInfo.getId());

        dbConnect.storeUser(userInfo.toString());
        // Set Vaadin Session attribute to current user
        VaadinSession.getCurrent().setAttribute("userID", userInfo.getId());
    }
}