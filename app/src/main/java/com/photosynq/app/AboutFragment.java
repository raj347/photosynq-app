package com.photosynq.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.model.BluetoothMessage;
import com.photosynq.app.model.Macro;
import com.photosynq.app.model.Protocol;
import com.photosynq.app.utils.BluetoothService;
import com.photosynq.app.utils.CommonUtils;
import com.photosynq.app.utils.Constants;
import com.photosynq.app.utils.PrefUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AboutFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AboutFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AboutFragment extends Fragment implements SelectDeviceDialogDelegate{
    private static int mSectionNumber;
    private String deviceAddress;
    private String mConnectedDeviceName;

    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int REQUEST_ENABLE_BT = 2;

    private OnFragmentInteractionListener mListener;
    TextView deviceId;
    TextView firmwareVersion;
    TextView mfgdate;
    BluetoothMessage bluetoothMessage;
    BluetoothService mBluetoothService;
    ProgressDialog progress;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AboutFragment.
     */
    public static AboutFragment newInstance(int sectionNumber ) {
        mSectionNumber = sectionNumber;
        AboutFragment fragment = new AboutFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        deviceAddress = CommonUtils.getDeviceAddress(getActivity());
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null != mBluetoothAdapter && !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        bluetoothMessage = new BluetoothMessage();
        bluetoothMessage.view = view;

        mBluetoothService = BluetoothService.getInstance(bluetoothMessage, mHandler);
        progress = ProgressDialog.show(getActivity(), "Please wait . . .", "Connecting to Multispeq device", true);
        try {
            if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                // Get the BLuetoothDevice object
                deviceAddress = CommonUtils.getDeviceAddress(getActivity());
                if(null == deviceAddress)
                {
                    Toast.makeText(getActivity(), "Measurement device not configured, Please configure measurement device (bluetooth).", Toast.LENGTH_SHORT).show();
                    selectDevice();
                }else {

                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                    mBluetoothService.connect(device);
                }
            } else {
                mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, BluetoothService.STATE_CONNECTED, 1).sendToTarget();
            }

            String appName = getString(R.string.app_name);
            String versionName = getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0).versionName;
            int versionCode = getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0).versionCode;
            String messageStr = appName + "\n\n" +
                    "Version " + versionName + "\n" +
                    Constants.SERVER_URL;

            TextView versionTv = (TextView) view.findViewById(R.id.version);
            TextView serverUrlTv = (TextView)view.findViewById(R.id.serverurl);
             deviceId = (TextView)view.findViewById(R.id.tv_device_id);
             firmwareVersion = (TextView)view.findViewById(R.id.tv_firmware_version);
             mfgdate = (TextView)view.findViewById(R.id.tv_manufacture_date);

            String ver = versionName+" ("+versionCode+")";
            versionTv.setText(ver);
            final SpannableString s =
                    new SpannableString(Constants.SERVER_URL);
            Linkify.addLinks(s, Linkify.WEB_URLS);

            serverUrlTv.setText(s);


        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return view;
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(mSectionNumber);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }

    private void selectDevice() {

        deviceAddress = CommonUtils.getDeviceAddress(getActivity());
        if (null == deviceAddress) {
            FragmentManager fragmentManager = getFragmentManager();
            SelectDeviceDialog selectDeviceDialog = new SelectDeviceDialog();
            selectDeviceDialog.show(fragmentManager, "Select Measurement Device", this);
        }
    }

    @Override
    public void onDeviceSelected(String result) {

        deviceAddress = result;
        if (null == deviceAddress) {
            Toast.makeText(getActivity(), "Measurement device not configured, Please configure measurement device (bluetooth).", Toast.LENGTH_SHORT).show();

            selectDevice();
        }

    }


    private void sendData(String data) {
        // Check that we're actually connected before trying anything

        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(getActivity().getApplicationContext(),"Not Connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (data.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send;
            send = data.getBytes();
            mBluetoothService.write(send);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth  services

        if (mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            mBluetoothService.stop();
        }

    }

    private final Handler mHandler = new Handler() {
        StringBuffer deviceinfo = new StringBuffer();
         boolean isJSONValid(String jsonStr) {
            try {
                new JSONObject(jsonStr);
            } catch (JSONException ex) {
                try {
                    new JSONArray(jsonStr);
                } catch (JSONException ex1) {
                    return false;
                }
            }
            return true;
        }
        @Override
        public void handleMessage(Message msg) {

//            BluetoothMessage bluetoothMessage = (BluetoothMessage) msg.obj;

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    if(Constants.D) Log.i("PHOTOSYNC", "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            //if(msg.arg2 == 1) { //Send measurement request
                            //mtvStatusMessage.setText(R.string.title_connected_to);
                            if (mConnectedDeviceName != null) {
                              //  mtvStatusMessage.append(mConnectedDeviceName);
                                sendData("1007");
                            }
                            //mtvStatusMessage.setText("Initializing measurement please wait ...");

                            //}
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            //mtvStatusMessage.setText(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            //mtvStatusMessage.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    break;
                case Constants.MESSAGE_STREAM:
//                    String reading = bluetoothMessage.message;
                    deviceinfo.append(msg.obj);
                    if(isJSONValid(deviceinfo.toString()))
                    {
                        try {
                            JSONObject deviceInfoJson = new JSONObject(deviceinfo.toString());
                            deviceId.setText(deviceInfoJson.getString("device_id"));
                            firmwareVersion.setText(deviceInfoJson.getString("firmware_version"));
                            mfgdate.setText(deviceInfoJson.getString("manufacture_date"));
                            mBluetoothService.stop();
                            progress.dismiss();
                        } catch (JSONException e) {}
                    }
                    break;
                case Constants.MESSAGE_READ:

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getActivity(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                    break;
                case Constants.MESSAGE_TOAST:
                    if(!msg.getData().getString(Constants.TOAST).trim().isEmpty()) {
                        Toast.makeText(getActivity().getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }

                    break;
                case Constants.MESSAGE_STOP:
                    Toast.makeText(getActivity(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    progress.dismiss();
                    //??mBluetoothService.stop();
                    break;
            }
        }
    };

}
