package com.duriseo.android.feature.settings.di

import com.duriseo.android.feature.settings.profile.photo.AndroidCameraAvailability
import com.duriseo.android.feature.settings.profile.photo.AndroidPickedPhotoReader
import com.duriseo.android.feature.settings.profile.photo.CameraAvailability
import com.duriseo.android.feature.settings.profile.photo.PickedPhotoReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** SCR-SET-04의 사진 읽기·카메라 앱 판정 구현 묶음. ViewModel은 인터페이스만 안다 (테스트에서 가짜로 바꾼다) */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfilePhotoModule {
    @Binds
    abstract fun bindPickedPhotoReader(impl: AndroidPickedPhotoReader): PickedPhotoReader

    @Binds
    abstract fun bindCameraAvailability(impl: AndroidCameraAvailability): CameraAvailability
}
