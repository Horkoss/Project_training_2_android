# Project Training 2 Android - KEYCAM

Projet Android for Project Training 2 at BJTU (Beijing Jiaotong University)

#### Motivation and ambition

The project was born during a Android Embedded Software Development course at Beijing Jiaotong University in China between two students who wanted to create an Android app entirely based on functionality. I (Jean-Luc) think to make an Android application since the first time I started to learn JAVA languages. In love with programming, I wanted to share it through an easy access application so everybody can join and try it. "KEYCAM" ambition is to place programming in the middle of a funny, entertaining so everybody even beginners can enjoy programming and hopefully start learning programming languages afterward.

## Description

KEYCAM is an application to control your device from a web site everywhere in the world. Nowadays, a lot of peoples change phone every year so them probably have an old phone in their house wasted at doing NOTHING. So we wanted to make a smart reuse of those smartphone so we created KeyCam. There is a lot of features, you can take pictures, see the video, vibrate the phone, write a message which will be spoken at loud, etc...

Example:

You want to see what’s going on in your house when you are out. You can simply place your smartphone somewhere in your house and start our “KEYCAM” application. Then you just take control of your phone from your computer or your current smart phone to see if there is anybody by taking a picture or just looking at the streamed video.

## Structure

    .
    ├── manifests                       	# AndroidManifest.xml
    ├── java                    		# File Java
    │   ├── activities		        # All activities
    │   ├── fragments			# All fragments
    │   ├── models			        # All models
    │   └── network                         # All network 
    ├── res					# File source for application
    │   ├── anim			        # SLide in/out
    │   ├── layout				# Template of all Activity
    │   └── values				# Styles and String
    └── Gradle Scripts			# Gradle 3.3

## Examples Codes

#### Exemples User Model

    /.../
    
    @Parcel
    public class UserModel {
      Boolean success;
      String token;
      String id;
      String message;

      public Boolean getSuccess() {
        return success;
      }

      public void setSuccess(Boolean success) {
        this.success = success;
      }
      /.../
    }
    
    /.../

Full code [here](https://github.com/Horkoss/Project_training_2_android/blob/master/app/src/main/java/com/keycam/models/UserModel.java)

#### Examples Main Activity

    private void loadLoginFragment(){
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        LoginFragment loginFragment = new LoginFragment();
        fragmentTransaction.replace(R.id.login_fragment_container, loginFragment, LoginFragment.TAG).commit();
    }
	
Full code [here](https://github.com/Horkoss/Project_training_2_android/blob/master/app/src/main/java/com/keycam/activities/MainActivity.java)

#### Examples Creat Account Fragment

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

Full code [here](https://github.com/Horkoss/Project_training_2_android/blob/master/app/src/main/java/com/keycam/fragments/CreateAccountFragment.java)

## List of functionnality

:white_check_mark: Graphics

:white_check_mark: Connection 
:white_check_mark: Register 
:x: Flash
:x: Stream video 
:x: Sound 
:x: Music 

## Team & Credits

[![Jean Luc](https://raw.githubusercontent.com/keysim/gearobot/master/doc/img/tang.png)](http://vireth.com) | [![Vireth](https://raw.githubusercontent.com/keysim/gearobot/master/doc/img/vireth.png)](http://vireth.com)
---|---
:chicken: [Jean Luc Tang](vireth.com) | :monkey: [Vireth Thach sok](vireth.com)

## License

[The MIT License](http://opensource.org/licenses/MIT)

Copyright (c) 2017 Vireth
