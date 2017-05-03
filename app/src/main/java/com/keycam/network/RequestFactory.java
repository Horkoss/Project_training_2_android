package com.keycam.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RequestFactory {
    private static final String BASE_URL = "http://163.5.84.197:4242/api/";

    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build();

    public static ApiEndPointInterface createApiCallRequest(){
        return retrofit.create(ApiEndPointInterface.class);
    }

/*    public static ApiError parseError(Response<?> response) {
        Converter<ResponseBody, ApiError> converter = retrofit.responseBodyConverter(ApiError.class, new Annotation[0]);

        ApiError error;
        Log.i("error request", response.errorBody().toString());

        try {
            error = converter.convert(response.errorBody());
        } catch (IOException e) {
            return new ApiError();
        }

        return error;
    }*/
}
