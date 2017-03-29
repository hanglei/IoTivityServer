package com.kmk.iotivityserver.resource;

import android.app.Activity;
import android.util.Log;

import org.iotivity.base.EntityHandlerResult;
import org.iotivity.base.ObservationInfo;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResourceHandle;
import org.iotivity.base.OcResourceRequest;
import org.iotivity.base.OcResourceResponse;
import org.iotivity.base.RequestHandlerFlag;
import org.iotivity.base.RequestType;
import org.iotivity.base.ResourceProperty;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LedResource implements OcPlatform.EntityHandler {
    private static final String TAG = LedResource.class.getSimpleName();
    private String mResourceUri;                //resource URI
    private String mResourceTypeName;           //resource type name.
    private String mResourceInterface;          //resource interface.
    private OcResourceHandle mResourceHandle;   //resource handle
    private List<Byte> mObservationIds;
    private Thread threadNotifier;
    private boolean isDoNotify = false;

    public LedResource() {
        mResourceUri = "/a/led";
        mResourceTypeName = "core.thing";
        mResourceInterface = OcPlatform.DEFAULT_INTERFACE;
        mResourceHandle = null;
    }

    public synchronized void registerResource() throws OcException {
        if (mResourceHandle == null) {
            mResourceHandle = OcPlatform.registerResource(mResourceUri, mResourceTypeName, mResourceInterface,
                    this, EnumSet.of(ResourceProperty.DISCOVERABLE, ResourceProperty.OBSERVABLE)
            );
        }
    }

    public synchronized void unregisterResource() throws OcException {
        if (null != mResourceHandle) {
            OcPlatform.unregisterResource(mResourceHandle);
        }
    }

    @Override
    public synchronized EntityHandlerResult handleEntity(final OcResourceRequest request) {
        EntityHandlerResult ehResult = EntityHandlerResult.ERROR;
        if (request == null) {
            Log.d(TAG, "Server request is invalid");
            return ehResult;
        }

        EnumSet<RequestHandlerFlag> requestFlags = request.getRequestHandlerFlagSet();
        if (requestFlags.contains(RequestHandlerFlag.INIT)) {
            Log.d(TAG, "Request Flag: Init");
            ehResult = EntityHandlerResult.OK;
        }
        if (requestFlags.contains(RequestHandlerFlag.REQUEST)) {
            Log.d(TAG, "Request Flag: Request");
            ehResult = handleRequest(request);
        }
        if (requestFlags.contains(RequestHandlerFlag.OBSERVER)) {
            Log.d(TAG, "Request Flag: Observer");
            ehResult = handleObserver(request);
        }
        return ehResult;
    }

    private EntityHandlerResult handleRequest(OcResourceRequest request) {
        EntityHandlerResult ehResult = EntityHandlerResult.ERROR;
        Map<String, String> queries = request.getQueryParameters();
        if (!queries.isEmpty()) {
            Log.d(TAG, "Query processing is up to entityHandler");
            for (Map.Entry<String, String> entry : queries.entrySet()) {
                Log.d(TAG, "Query key: " + entry.getKey() + " value: " + entry.getValue());
            }
        } else {
            Log.d(TAG, "No query parameters in this request");
        }

        RequestType requestType = request.getRequestType();
        switch (requestType) {
            case GET:
                Log.d(TAG, "Request Type is GET");
                ehResult = handleGetRequest(request);
                break;
            case POST:
                Log.d(TAG, "Request Type is POST");
                ehResult = handlePostRequest(request);
                break;
            case PUT:
                Log.d(TAG, "Request Type is PUT");
                ehResult = handlePutRequest(request);
                break;
            case DELETE:
                Log.d(TAG, "Request Type is DELETE");
                ehResult = handleDeleteRequest(request);
                break;
        }
        return ehResult;
    }

    private EntityHandlerResult handleObserver(final OcResourceRequest request) {
        ObservationInfo observationInfo = request.getObservationInfo();
        switch (observationInfo.getObserveAction()) {
            case REGISTER:
                if (mObservationIds == null) {
                    mObservationIds = new LinkedList<>();
                }
                mObservationIds.add(observationInfo.getOcObservationId());
                break;
            case UNREGISTER:
                mObservationIds.remove(observationInfo.getOcObservationId());
                break;
        }
        if (threadNotifier == null) {
            threadNotifier = new Thread(new Runnable() {
                public void run() {
                    notifyObservers(request);
                }
            });
            threadNotifier.start();
        }
        return EntityHandlerResult.OK;
    }

    private void notifyObservers(OcResourceRequest request) {
        while (true) {
            try {
                if (isDoNotify) {
                    OcResourceResponse response = new OcResourceResponse();
                    response.setErrorCode(200);
                    response.setResourceRepresentation(getOcRepresentation());
                    OcPlatform.notifyListOfObservers(mResourceHandle, mObservationIds, response);
                } else {
                    OcPlatform.notifyAllObservers(mResourceHandle);
                }
            } catch (OcException e) {
                e.printStackTrace();
            }
        }
    }

    private EntityHandlerResult handleGetRequest(final OcResourceRequest request) {
        OcResourceResponse response = new OcResourceResponse();
        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());
        response.setResourceRepresentation(getOcRepresentation());
        response.setResponseResult(EntityHandlerResult.OK);
        response.setErrorCode(200);
        return sendResponse(response);
    }

    private EntityHandlerResult handlePostRequest(OcResourceRequest request) {
        OcResourceResponse response = new OcResourceResponse();
        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());
        try {
            Log.d(TAG, "" + request.getResourceRepresentation().getValue("what"));
        } catch (OcException e) {
            e.printStackTrace();
        }
        response.setResourceRepresentation(getOcRepresentation());
        response.setResponseResult(EntityHandlerResult.OK);
        response.setErrorCode(200);
        return sendResponse(response);
    }

    private EntityHandlerResult handlePutRequest(OcResourceRequest request) {
        return EntityHandlerResult.OK;
    }

    private EntityHandlerResult handleDeleteRequest(OcResourceRequest request) {
        return EntityHandlerResult.OK;
    }

    private EntityHandlerResult sendResponse(OcResourceResponse response) {
        try {
            OcPlatform.sendResponse(response);
            return EntityHandlerResult.OK;
        } catch (OcException e) {
            e.printStackTrace();
            return EntityHandlerResult.ERROR;
        }
    }

    public OcRepresentation getOcRepresentation() {
        OcRepresentation ocRepresentation = new OcRepresentation();
        try {
            ocRepresentation.setValue("result", "1");
        } catch (OcException e) {
            e.printStackTrace();
        }
        return ocRepresentation;
    }
}