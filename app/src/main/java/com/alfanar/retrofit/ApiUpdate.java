package com.alfanar.retrofit;

import com.alfanar.dto.DtoApkModel;
import com.alfanar.dto.DtoMacAddressModel;
import com.alfanar.villaroom.models.ControlServiceModel;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiUpdate {


    @POST("root/getMac")
    Call<DtoMacAddressModel> getMacByCpu(@Body DtoMacAddressModel model);

    @POST("root/getApk")
    Call<DtoApkModel> getApk(@Body DtoApkModel model);

    @POST("root/controlService")
    Call<String> netControl(@Body ControlServiceModel model);




}
