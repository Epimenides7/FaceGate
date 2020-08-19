cmake_minimum_required(VERSION 3.4.1)
set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

#openmp
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fopenmp -static-openmp")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fopenmp -static-openmp")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -fopenmp -static-openmp")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fomit-frame-pointer -fstrict-aliasing -ffast-math -fvisibility=hidden" )
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fomit-frame-pointer -fstrict-aliasing -ffast-math -fvisibility=hidden -fvisibility-inlines-hidden")

# disable rtti and exceptions
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti -fno-exceptions")



#source directory源文件目录
file(GLOB FACE_SRC src/main/cpp/ncnn_cpp/*.h
		src/main/cpp/ncnn_cpp/*.cpp)

# 将上面的FACE_SRC变量赋值给FACA_COMPILE_CODE变量
set(FACE_COMPILE_CODE ${FACE_SRC})

#include头文件目录
include_directories(src/main/cpp/mnn_include
					src/main/cpp/ncnn_include
					src/main/cpp/mnn_cpp)

#添加MMN库
add_library(MNN SHARED IMPORTED)
add_library(MNN_CL SHARED IMPORTED)
#添加NCNN库
add_library(libncnn STATIC IMPORTED)
#编译为动态库
add_library(NCNN_Face SHARED ${FACE_COMPILE_CODE})

set_target_properties(libncnn
	PROPERTIES IMPORTED_LOCATION
	${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libncnn.a)

set_target_properties(MNN
  PROPERTIES IMPORTED_LOCATION
  ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libMNN.so)
set_target_properties(MNN_CL
  PROPERTIES IMPORTED_LOCATION
  ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libMNN_CL.so)

add_library(facedetect SHARED src/main/cpp/mnn_cpp/Ultra_jni.cpp
		src/main/cpp/mnn_cpp/UltraFace.cpp
		src/main/cpp/mnn_cpp/net.cpp)

#添加工程所依赖的库
find_library(log-lib log)
find_library( jnigraphics-lib jnigraphics)

add_definitions(-DMNN_USE_LOGCAT)

target_link_libraries(facedetect
                       MNN
					   MNN_CL
                       jnigraphics
                       z
                       ${log-lib})
target_link_libraries( NCNN_Face
						libncnn
						jnigraphics
						z
						${log-lib} )
