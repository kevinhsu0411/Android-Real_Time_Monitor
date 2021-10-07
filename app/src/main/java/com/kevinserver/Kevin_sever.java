package com.kevinserver;

import static com.lidroid.xutils.util.MimeTypeUtils.getMimeType;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import elonen.NanoHTTPD;
import elonen.NanoHTTPD.Response;
import elonen.router.RouterNanoHTTPD;

public class Kevin_sever implements RouterNanoHTTPD.UriResponder {

    @Override
    public NanoHTTPD.Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        Log.d("kk", "Response get");
        Response response = null;
        //File target = new File(MainActivity.activity.getFilesDir() + "/kevin.jpg");
        String mimeType = getMimeType("target.getName()");
        try {
            //FileInputStream fis = new FileInputStream(target);
            //response = NanoHTTPD.newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, fis, 1000000);
            response = NanoHTTPD.newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, null, 1000000);

        } catch (Exception e) {

        }
        return response;
    }

    @Override
    public NanoHTTPD.Response put(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response post(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response delete(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response other(String method, RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return null;
    }
}
