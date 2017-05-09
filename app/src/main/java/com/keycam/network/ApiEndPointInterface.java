package com.keycam.network;

import com.google.gson.JsonObject;
import com.keycam.models.UserModel;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiEndPointInterface {
    @POST("authenticate")
    Call<UserModel> login(@Body JsonObject jsonObject);

    @POST("register")
    Call<UserModel> register(@Body JsonObject jsonObject);
}
