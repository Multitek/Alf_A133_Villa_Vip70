cmake_minimum_required(VERSION 3.10.2)

add_library(I2CTransfer SHARED
            I2CTransfer.c)

# Include libraries needed for hello-jnicallback lib
target_link_libraries(I2CTransfer
                      android
                      log)
