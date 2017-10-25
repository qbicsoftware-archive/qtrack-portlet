package com.vaadin.model;

import com.vaadin.server.*;
import com.vaadin.ui.*;

import java.io.IOException;

/**
 * Handles the VaadinRequests and sets the authorization request for the Authentication Process
 */
public class ReturnCodeHandler implements RequestHandler{

    private AuthRequest authRequest = null;

    /**
     * @param session the VaadinSession
     * @param request the VaadinRequest
     * @param response the VaadinResponse
     * @return whether the request could be handled successfully
     */
    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response) throws IOException {
        if (request.getParameter("code") != null) {
            String code = request.getParameter("code");
            authRequest.handleReturnCode(code);
            VaadinSession.getCurrent().removeRequestHandler(this);

            String redirectURL = "http://localhost:8080";
            ((VaadinServletResponse) response).getHttpServletResponse().
                    sendRedirect(redirectURL);

            // Set UI Content to Main View
            MainView mainView = new MainView();
            VaadinSession.getCurrent().getUIById(0).setContent(mainView);
            return true;
        }
        return false;
    }

    /**
     * sets the authorization request
     * @param authRequest to set
     */
    public void setAuthRequest(AuthRequest authRequest) {
        this.authRequest = authRequest;
    }
}
