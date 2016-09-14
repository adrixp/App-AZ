package az.lahermandad.com.azhermandad;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.Bind;

/**
 * Created by PIZARROSAEXT on 13/09/2016.
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static String Error = "";
    private static final int REQUEST_SIGNUP = 0;

    @Bind(R.id.input_email) EditText _emailText;
    @Bind(R.id.input_password) EditText _passwordText;
    @Bind(R.id.btn_login) Button _loginButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                // By default we just finish the Activity and log them in automatically
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        _loginButton.setEnabled(true);
        Intent mainIntent = new Intent(this,Menu.class);
        startActivity(mainIntent);
        finish();
    }

    public void makeRequest (String email, String pass){

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.logAuth));
        progressDialog.show();

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://test.selltickets.lahermandad.es/api/user";

        String strTo64 = email + ":" + pass;
        byte[] b64 = Base64.encode(strTo64.getBytes(),Base64.DEFAULT);
        final String valHeader = "Basic " + new String(b64);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        //Toast.makeText(getBaseContext(), "GET: Response is: "+ response.substring(0,500), Toast.LENGTH_LONG).show();
                        System.out.print("Response: " + response);

                        new android.os.Handler().postDelayed(
                                new Runnable() {
                                    public void run() {
                                        // On complete call either onLoginSuccess or onLoginFailed
                                        onLoginSuccess();
                                        //onLoginFailed();
                                        progressDialog.dismiss();
                                    }
                                }, 2000);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                System.out.print("Error: " + error.getMessage());
                Error = getString(R.string.logUsernotExist);
                new android.os.Handler().postDelayed(
                        new Runnable() {
                            public void run() {
                                onLoginFailed();
                                progressDialog.dismiss();
                            }
                        }, 2000);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> mHeaders = new ArrayMap<String, String>();
                mHeaders.put("Authorization", valHeader);
                return mHeaders;
            }
        };
        // Add the request to the RequestQueue.
        queue.add(stringRequest);


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
