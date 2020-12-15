package com.malcolmmaima.dishiapp.Controller.Utils;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PullImages {

    /**
     * Regular expression to match file types  - .png/.jpg/.gif
     */
    public static final Pattern resources = Pattern.compile("([^\"'\n({}]+\\.(png|jpg|gif))",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    /**
     * Pulls out "resources" from the provided text.
     */
    public static Set<String> findResources(URL url, String text, Boolean multiple) {
        Matcher matcher = resources.matcher(text);
        Set<String> resources = new HashSet<>();

        //get multiple images found in specified page/content block
        if(multiple){
            while (matcher.find()) {
                String resource = matcher.group(1);
                String urlStr = url.toString();
                int endIndex = urlStr.lastIndexOf("/") + 1;
                String parentPath = endIndex > 0 ? urlStr.substring(0, endIndex) : urlStr;
                String fqResource = resource.startsWith("//") ? url.getProtocol() + ":" + resource :
                        resource.startsWith("http") ? resource
                                : resource.startsWith("/") ? getBaseUrl(url) + resource : parentPath + resource;
                if (fqResource.contains("?")) {
                    fqResource = fqResource.substring(0, fqResource.indexOf("?"));
                }
                resources.add(fqResource);
            }
        }

        //get single image ... first one
        else {
            if (matcher.find()) {
                String resource = matcher.group(1);
                String urlStr = url.toString();
                int endIndex = urlStr.lastIndexOf("/") + 1;
                String parentPath = endIndex > 0 ? urlStr.substring(0, endIndex) : urlStr;
                String fqResource = resource.startsWith("//") ? url.getProtocol() + ":" + resource :
                        resource.startsWith("http") ? resource
                                : resource.startsWith("/") ? getBaseUrl(url) + resource : parentPath + resource;
                if (fqResource.contains("?")) {
                    fqResource = fqResource.substring(0, fqResource.indexOf("?"));
                }
                resources.add(fqResource);
            }
        }

        return resources;
    }

    private static String getBaseUrl(URL url) {
        return String.format("%s://%s:%s", url.getProtocol(), url.getHost(), getPort(url));
    }

    private static int getPort(URL url) {
        int port = url.getPort();
        return port != -1 ? port : "https".equals(url.getProtocol()) ? 443 : 80;
    }
}
