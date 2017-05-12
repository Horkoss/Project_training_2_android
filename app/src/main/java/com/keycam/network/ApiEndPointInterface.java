package com.keycam.network;

import com.google.gson.JsonObject;
import com.keycam.models.UserModel;
import com.keycam.models.VideoSessionModel;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiEndPointInterface {
    @POST("authenticate")
    Call<UserModel> login(@Body JsonObject jsonObject);

    @POST("register")
    Call<UserModel> register(@Body JsonObject jsonObject);

    @GET("video")
    Call<VideoSessionModel> getSession();
}
