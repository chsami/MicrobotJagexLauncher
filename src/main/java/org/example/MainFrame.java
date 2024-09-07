package org.example;

import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLifeSpanHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class MainFrame {

    public static CefClient client;
    public static JTabbedPane tabbedPane;

    /**
     * To display a simple browser window, it suffices completely to create an
     * instance of the class CefBrowser and to assign its UI component to your
     * application (e.g. to your content pane).
     * But to be more verbose, this CTOR keeps an instance of each object on the
     * way to the browser UI.
     */
    public static void create(String startURL, boolean useOSR, boolean isTransparent, String[] args) throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException {
        // Initialize CEF with command line arguments.
        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = false;
        CefApp cefApp = CefApp.getInstance(settings);

        // Create the client and set the main window as the parent.
        client = cefApp.createClient();

        client.addRequestHandler(new CustomRequestHandler());

        // Create the browser instance.
        String url = startURL;
        CefBrowser browser = client.createBrowser(url, false, false);
        Component browserUI = browser.getUIComponent();

        // Create the main window.
        JFrame frame = new JFrame();
        tabbedPane = new JTabbedPane();
        tabbedPane.add(browserUI, "Login");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);

        frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
        // Add a life span handler to handle browser close events.
        client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public void onBeforeClose(CefBrowser browser) {
                cefApp.dispose();
                System.exit(0);
            }
        });

        // Shutdown hook to make sure CEF is disposed properly.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> cefApp.dispose()));
    }

    public static void openNewTab(String startURL) {
        if (client == null) {
            System.out.println("Something went wrong! Client is null");
        }

        // Create a new browser instance
        CefBrowser browser = client.createBrowser(startURL, false, false);

        // Create a new panel to hold the browser's UI component
        JPanel browserPanel = new JPanel(new BorderLayout());
        browserPanel.add(browser.getUIComponent(), BorderLayout.CENTER);

        // Add the panel as a new tab in the tabbed pane
        tabbedPane.addTab("Tab " + (tabbedPane.getTabCount() + 1), browserPanel);

        // Optionally, focus the new tab
        tabbedPane.setSelectedComponent(browserPanel);

        // Handle browser life span events (e.g., closing)
        client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                super.onAfterCreated(browser);
                System.out.println("New tab created with URL: " + startURL);
            }

            @Override
            public boolean doClose(CefBrowser browser) {
                System.out.println("Tab is closing.");
                return super.doClose(browser);
            }
        });
    }

    public static void main(String[] args) throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException {
        //Print some info for the test reports. You can ignore this.

        // The simple example application is created as anonymous class and points
        // to Google as the very first loaded page. Windowed rendering mode is used by
        // default. If you want to test OSR mode set |useOsr| to true and recompile.

    }
}
