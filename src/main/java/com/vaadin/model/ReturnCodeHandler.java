package com.vaadin.model;

import com.vaadin.server.*;
import com.vaadin.ui.*;

import java.io.IOException;

/**
 * Created by caspar on 06.06.17.
 */
public class ReturnCodeHandler implements RequestHandler{

    AuthRequest authRequest = null;
    final String redirectURL = "http://localhost:8080";

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response) throws IOException {
        if (request.getParameter("code") != null) {
            String code = request.getParameter("code");
            authRequest.handleReturnCode(code);
            VaadinSession.getCurrent().removeRequestHandler(this);

            ((VaadinServletResponse) response).getHttpServletResponse().
                    sendRedirect(redirectURL);

            // Set UI Content to Main View
            MainView mainView = new MainView();
            VaadinSession.getCurrent().getUIById(0).setContent(mainView);
            return true;
        }
        return false;
    }

    public void authDenied(String reason) {
        Notification.show("authDenied:" + reason,
                Notification.Type.ERROR_MESSAGE);
    }

    public void setAuthRequest(AuthRequest authRequest) {
        this.authRequest = authRequest;
    }
}
