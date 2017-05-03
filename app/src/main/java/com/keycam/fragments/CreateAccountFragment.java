package com.keycam.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.keycam.R;
import com.keycam.models.UserModel;
import com.keycam.network.ApiEndPointInterface;
import com.keycam.network.RequestFactory;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.view.View.GONE;

/**
 * Show a form that let the user to create an account
 */
public class CreateAccountFragment extends Fragment {
    public static final String TAG = CreateAccountFragment.class.getName();

    private Unbinder mUnbinder;

    @BindView(R.id.email)
    TextInputEditText mEmail;

    @BindView(R.id.password)
    TextInputEditText mPassword;

    @BindView(R.id.sign_up)
    Button mSignUp;

    @BindView(R.id.progress)
    ProgressBar mProgressBar;

    /**
     * Make asynchronous api request
     */
    @OnClick(R.id.sign_up)
    void signUp(){
        if (mEmail.getText().length() < 1) {
            Toast.makeText(getActivity(), "Email must not be empty", Toast.LENGTH_SHORT).show();
        } else if (mPassword.getText().length() > 20 || mPassword.getText().length() < 6) {
            Toast.makeText(getActivity(), "Password wrong. It have to be between 6 and 20 characters", Toast.LENGTH_SHORT).show();
        } else {
            ApiEndPointInterface apiRequest = RequestFactory.createApiCallRequest();
            JsonObject jsonObject = createJsonFromInfos();
            showProgressBar();
            Call<UserModel> call = apiRequest.createAccount(jsonObject);
            call.enqueue(new Callback<UserModel>() {
                @Override
                public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                    parseResponse(response);
                }

                @Override
                public void onFailure(Call<UserModel> call, Throwable t) {
                    hideProgressBar();
                    Toast.makeText(getActivity(), "Sign up failed, please check your network", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.create_account_fragment, container, false);
        mUnbinder = ButterKnife.bind(this, view);

        return view;
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

    /**
     * Parse the response from the api
     * @param response response of the api
     */
    private void parseResponse(Response<UserModel> response){
        int statusCode = response.code();

        if (statusCode == 201) { // Success
            getActivity().getSupportFragmentManager().popBackStack();
            Toast.makeText(getActivity(), "Account created", Toast.LENGTH_SHORT).show();
        } else if (statusCode == 200 || statusCode >= 300 && statusCode < 500){
            Toast.makeText(getActivity(), response.body().getMessage(), Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(getActivity(), "Please try again later", Toast.LENGTH_SHORT).show();
        hideProgressBar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    private void showProgressBar(){
        mSignUp.setVisibility(GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar(){
        mSignUp.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(GONE);
    }
}
