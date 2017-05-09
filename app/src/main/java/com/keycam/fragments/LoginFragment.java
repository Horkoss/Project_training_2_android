package com.keycam.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.keycam.R;
import com.keycam.activities.HomeActivity;
import com.keycam.activities.KeeperActivity;
import com.keycam.models.UserModel;
import com.keycam.network.ApiEndPointInterface;
import com.keycam.network.ApiError;
import com.keycam.network.RequestFactory;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.view.View.GONE;

/**
 * A login screen that offers login via email/password.
 */
public class LoginFragment extends Fragment {
    public static final String TAG = LoginFragment.class.getName();

    @BindView(R.id.email)
    TextInputEditText mEmail;

    @BindView(R.id.password)
    TextInputEditText mPassword;

    @BindView(R.id.sign_in_baby)
    Button mSignInBaby;

    @BindView(R.id.create_account)
    Button mCreateAccount;

    @BindView(R.id.progress)
    ProgressBar mProgressBar;

    @BindView(R.id.buttons_layout)
    LinearLayout mButtonsLayout;

    private Unbinder mUnbinder;

    @OnClick(R.id.sign_in_baby)
    public void signInBaby(){
        hideKeyboard();
        ApiEndPointInterface apiRequest = RequestFactory.createApiCallRequest();
        JsonObject jsonObject = createJsonFromInfos();
        storeEmail();
        showProgressBar();
        Call<UserModel> call = apiRequest.login(jsonObject);
        call.enqueue(new Callback<UserModel>() {
            @Override
            public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                parseResponse(response);
            }

            @Override
            public void onFailure(Call<UserModel> call, Throwable t) {
                hideProgressBar();
                Toast.makeText(getActivity(), "Sign in failed, please check your network", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick(R.id.sign_in_parent)
    public void signInParent() {
        Intent intent = new Intent(getActivity(), KeeperActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        getActivity().finish();
    }

    /**
     * Show the RegisterFragment with animation
     */
    @OnClick(R.id.create_account)
    public void loadCreateAccountFragment(){
        hideKeyboard();
        RegisterFragment registerFragment = new RegisterFragment();
        getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                R.anim.slide_out_left, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.login_fragment_container, registerFragment, RegisterFragment.TAG)
                .addToBackStack(null).commit();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.login_fragment, container, false);
        mUnbinder = ButterKnife.bind(this, view);

        putStoredUserEmail();

        return view;
    }

    private void putStoredUserEmail(){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("shared", Context.MODE_PRIVATE);
        mEmail.setText(sharedPreferences.getString("email", ""));
    }

    /**
     * Get informations from the form and put in a json
     * @return json that will be send in the request
     */
    private JsonObject createJsonFromInfos(){
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("email", mEmail.getText().toString());
        jsonObject.addProperty("password", mPassword.getText().toString());

        return jsonObject;
    }

    private void parseResponse(Response<UserModel> response){
        int statusCode = response.code();

        Log.d("STATUS CODE", String.valueOf(statusCode));
        if (statusCode == 200) { // Success
            Intent intent = new Intent(getActivity(), HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("user", Parcels.wrap(response.body()));
            startActivity(intent);
            getActivity().finish();
        }
        else if (statusCode >= 300 && statusCode < 500){
            ApiError apiError = RequestFactory.parseError(response);
            Toast.makeText(getActivity(), apiError.getMessage(), Toast.LENGTH_SHORT).show();        }
        else
            Toast.makeText(getActivity(), "Sign in failed, try again later", Toast.LENGTH_SHORT).show();
        hideProgressBar();
    }

    private void showProgressBar(){
        mButtonsLayout.setVisibility(GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar(){
        mButtonsLayout.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(GONE);
    }
    private void storeEmail(){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("shared", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", mEmail.getText().toString()).apply();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}

