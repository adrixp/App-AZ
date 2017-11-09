package az.lahermandad.com.azhermandad;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.Bind;

/**
 * Created by PIZARROSAEXT on 13/09/2016.
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static String Error = "<Unexpected>";

    //private final String url = "https://test-sell-ticket.herokuapp.com/user/";
    private static String url ="https://sell-ticket.herokuapp.com/user/";

    String path = Environment.getExternalStorageDirectory() + "/AZ_LaHermandad";
    String token = "";
    boolean canReserve = false;

    @Bind(R.id.input_email) EditText _emailText;
    @Bind(R.id.input_password) EditText _passwordText;
    @Bind(R.id.btn_login) Button _loginButton;
    @Bind(R.id.checkBox)  CheckBox _checkBoxn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        Log.d(TAG, "path:   " + path);

        File folder = new File(path);
        if(!folder.isDirectory()){
            if(folder.mkdir()){
                Log.i(TAG, "Folder created successfully");
            }else{
                Log.i(TAG, "Folder couldn't be created");
            }
        }

        File file = new File(path, "tmp");
        //Read text from file
        String line = "empty";

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            line = br.readLine();
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            line = "empty";
        }

        if(line != null ){
            if(!line.equals("empty")) {
                byte[] data = Base64.decode(line, Base64.DEFAULT);
                String text = new String(data);
                String[] parts = text.split(":");

                _emailText.setText(parts[0]);
                _passwordText.setText(parts[1]);
            }
        }

        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

    }

    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }
        _loginButton.setEnabled(false);

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        // TODO: Implement your own authentication logic here.
        makeRequest(email,password);
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        _loginButton.setEnabled(true);
        Intent mainIntent = new Intent(this,Menu.class);
        mainIntent.putExtra("token", token);
        mainIntent.putExtra("canReserve", canReserve);
        startActivity(mainIntent);
    }


    public void makeRequest (String email, String pass){

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.logAuth));
        progressDialog.show();

        String strTo64 = email + ":" + pass;
        byte[] b64 = Base64.encode(strTo64.getBytes(),Base64.DEFAULT);

        loginRemember(b64);
        sendPost(email,pass,progressDialog);
    }

    public void loginRemember(byte[] b64){
        if(_checkBoxn.isChecked()){
            try {
                File settings = new File(path, "tmp");
                FileOutputStream fos2 = new FileOutputStream(settings);

                fos2.write(new String(b64).getBytes());
                fos2.close();

            } catch (java.io.IOException e) {
                Log.d(TAG, "Exception in writeFile", e);
            }
        }else if(!_checkBoxn.isChecked()){
            try {
                File settings = new File(path, "tmp");
                FileOutputStream fos2 = new FileOutputStream(settings);
                fos2.write("empty".getBytes());
                fos2.close();

            } catch (java.io.IOException e) {
                Log.d(TAG, "Exception in photoCallback", e);
            }
        }
    }

    public void sendPost(String email, String pass, final ProgressDialog progressDialog) {
         final RequestQueue queue = Volley.newRequestQueue(this);

        String strPost = "{  \n" +
                "  \"email\":\"" + email + "\",\n" +
                "  \"pass\": \"" + pass + "\"\n" +
                "}";

        String strMe = "{}";

        try {

            JSONObject jsonObjMe = new JSONObject(strMe);
            final JsonObjectRequest jsonArrayRequestMe = new JsonObjectRequest(Request.Method.GET, url + "me", jsonObjMe, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "Response OK post: " + response);
                    try {
                        String arr = response.getString("profiles");
                        String[] items = arr.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");

                        for(String s:items){
                            if (s.equals("\"CAN_RESERVE\"")){
                                canReserve = true;
                                break;
                            }
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    Log.d(TAG, "token: " + token);

                    new android.os.Handler().postDelayed( new Runnable() { public void run() {
                        onLoginSuccess();
                        progressDialog.dismiss(); } }, 1000);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "Response FAIL post: " + error);
                    NetworkResponse networkResponse = error.networkResponse;
                    if(networkResponse != null){


                        switch (networkResponse.statusCode){
                            case 401:
                                Error = "ME: Usuario o Pass Incorrecta";
                                break;
                            default:
                                Error = "ME: Error devuelto: " + networkResponse.statusCode + "\n" + networkResponse.toString();
                                break;
                        }
                    }

                    new android.os.Handler().postDelayed( new Runnable() { public void run() {
                        onLoginFailed();
                        progressDialog.dismiss(); } }, 1000);
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> mHeaders = new ArrayMap<String, String>();
                    mHeaders.put("authorization", "Bearer " + token);
                    return mHeaders;
                }
            };

            JSONObject jsonObj = new JSONObject(strPost);
            JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(Request.Method.POST, url + "login", jsonObj, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "Response OK post: " + response);
                    try {
                        token = response.getString("jwt");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "token: " + token);

                    queue.add(jsonArrayRequestMe);

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "Response FAIL post: " + error);

                    NetworkResponse networkResponse = error.networkResponse;
                    if(networkResponse != null){


                        switch (networkResponse.statusCode){
                            case 401:
                                Error = "LOGIN: Usuario o Pass Incorrecta";
                                break;
                            default:
                                Error = "LOGIN: Error devuelto: " + networkResponse.statusCode + "\n" + networkResponse.toString();
                                break;
                        }
                    }

                    new android.os.Handler().postDelayed( new Runnable() { public void run() {
                        onLoginFailed();
                        progressDialog.dismiss(); } }, 500);

                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> mHeaders = new ArrayMap<String, String>();
                    mHeaders.put("Content-Type", "application/json");
                    return mHeaders;
                }
            };

            queue.add(jsonArrayRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), getString(R.string.logFailDue) + " " + Error, Toast.LENGTH_LONG).show();

        _loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError(getString(R.string.logPutValidEm));
            Error = getString(R.string.logInvEm);
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() /*|| password.length() < 4 || password.length() > 10*/) {
            Error = getString(R.string.logNoPass);
            _passwordText.setError(Error);

            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

}
