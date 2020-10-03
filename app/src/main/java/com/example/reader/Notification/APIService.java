package com.example.reader.Notification;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers (
        {
            "Countent-Type:application/json",
            "Authorization:key=AAAAJ0Jq0cU:APA91bHoWJ1h6GSOloGRvqfzleQ4vQNvpDUoufhYVFLI5SJa9D3rYvVfv9MzZqrARf3vp4ccdV7e3V8NtG0E5_oUrcBjE663-JOqaTz9AnLexI4LM0x5NL4_Nc6uWQA1aLJYbFQd2eYD"
        })

    @POST("fcm/send")
    Call<MyResponse>sendNotification(@Body Sender body);
}
