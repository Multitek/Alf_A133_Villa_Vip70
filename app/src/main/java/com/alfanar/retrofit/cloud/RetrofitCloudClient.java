package com.alfanar.retrofit.cloud;

import com.alfanar.retrofit.BasicAuthInterceptor;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitCloudClient {

    private static volatile Retrofit retrofit;

    private RetrofitCloudClient() {}

    public static Retrofit getClient() {
        if (retrofit == null) {
            synchronized (RetrofitCloudClient.class) {
                if (retrofit == null) {
                    retrofit = createRetrofit();
                }
            }
        }
        return retrofit;
    }

    private static Retrofit createRetrofit() {
        try {
            X509TrustManager trustAllWithExpiryBypass = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    if (chain != null && chain.length > 0) {
                        X509Certificate cert = chain[0];
                        try {
                            cert.checkValidity();
                        } catch (CertificateExpiredException e) {
                            Logger.d("⚠️ RetrofitCloudClient.Sertifika süresi dolmuş, ama bağlantıya izin veriliyor: " + cert.getSubjectDN());
                        } catch (CertificateNotYetValidException e) {
                            Logger.d("⚠️ RetrofitCloudClient.Sertifika henüz geçerli değil, ama bağlantıya izin veriliyor: " + cert.getSubjectDN());
                        }
                    }
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2", "Conscrypt");
            sslContext.init(null, new TrustManager[]{trustAllWithExpiryBypass}, new SecureRandom());

            //final SSLContext sslContext = SSLContext.getInstance("SSL");
            //sslContext.init(null,  new TrustManager[]{trustAllWithExpiryBypass}, new java.security.SecureRandom());


            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new BasicAuthInterceptor(MyUtils.API_USER, MyUtils.API_PASS))
                    .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
                    .protocols(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1))
                    .sslSocketFactory(sslContext.getSocketFactory(), trustAllWithExpiryBypass)
                    .hostnameVerifier((hostname, session) ->
                            MyUtils.CLOUD_SERVER_DOMAIN.equalsIgnoreCase(hostname))
                    .retryOnConnectionFailure(true)
                    .callTimeout(60, TimeUnit.SECONDS)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Gson gson = new GsonBuilder()
                    .setStrictness(Strictness.LENIENT)
                    .setPrettyPrinting()
                    .create();
            return new Retrofit.Builder()
                    .baseUrl(MyUtils.SERVER_API_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("RetrofitCloudClient.Retrofit init failed: " + e.getMessage(), e);
        }
    }


}


