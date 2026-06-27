package com.viewsonic.classswift.windowframework.core.utils

import android.util.Log
import com.viewsonic.classswift.utils.DisplayUtils
import com.viewsonic.classswift.windowframework.core.CSWindowManager.getWindow
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.RelatedPosition
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.Horizontal
import com.viewsonic.classswift.windowframework.core.enums.WindowTag

object LocationUtil {

    fun gravityToLocation(gravity: Gravity, windowSize: SizeInPixels): Location {
        return when (gravity) {
            Gravity.CENTER_TOP -> {
                getWindowCenterTopPosition(windowSize)
            }
            Gravity.CENTER -> {
                getWindowCenterPosition(windowSize)
            }
            Gravity.CENTER_BOTTOM -> {
                getWindowCenterBottomPosition(windowSize)
            }
            else -> {
                getTopStartPosition()
            }
        }
    }

    fun getTopStartPosition(): Location {
        return Location(0, 0)
    }

    fun getWindowCenterTopPosition(windowSize: SizeInPixels): Location {
        val (deviceWidth, _) = DisplayUtils.getScreenSize(isIgnoreNavigationBars = true, isIgnoreStatusBars = true)
        val locationX = deviceWidth/2 - windowSize.width/2
        val locationY = 0

        return Location(locationX, locationY)
    }

    fun getWindowCenterPosition(windowSize: SizeInPixels): Location {
        val (deviceWidth, deviceHeight) = DisplayUtils.getScreenSize(isIgnoreNavigationBars = true, isIgnoreStatusBars = true)
        val locationX = deviceWidth/2 - windowSize.width/2
        val locationY = deviceHeight/2 - windowSize.height/2

        return Location(locationX, locationY)
    }

    fun getWindowCenterBottomPosition(windowSize: SizeInPixels): Location {
        val (deviceWidth, deviceHeight) = DisplayUtils.getScreenSize(isIgnoreNavigationBars = true, isIgnoreStatusBars = true)
        val locationX = deviceWidth/2 - windowSize.width/2
        val locationY = deviceHeight - windowSize.height

        return Location(locationX, locationY)
    }

    fun calculateSubWindowLocation(
        subWindowSize: SizeInPixels,
        mainWindowTag: WindowTag,
        anchorX: Int,
        relatedPosition: RelatedPosition
    ): Location {
        // 計算水平位置
        val coordinateX = when (relatedPosition.horizontal) {
            Horizontal.LEFT -> anchorX - subWindowSize.width
            Horizontal.CENTER -> anchorX - (subWindowSize.width / 2)
            Horizontal.RIGHT -> anchorX
        }

        // 計算垂直位置
        var coordinateY = DisplayUtils.getScreenSize(isIgnoreNavigationBars = true, isIgnoreStatusBars = true).second - subWindowSize.height
        val mainWindowLocation = getWindow(mainWindowTag)?.getWindowConfig()?.location
        if (mainWindowLocation != null) {
        // Timber.d("mainWindowLocation: x-${mainWindowLocation.coordinateX}, y-${mainWindowLocation.coordinateY}")
            coordinateY = mainWindowLocation.coordinateY - relatedPosition.marginVerticalInPixels - subWindowSize.height
        }

        // Timber.d("subWindowLocation: x-${coordinateX}, y-${coordinateY}")
        return Location(coordinateX, coordinateY)
    }

    /**
     * 需要將 Window 座標校正回螢幕內時使用
     */
    fun correctLocationInsideScreen(coordinateX: Int, coordinateY: Int, mainWindowSize: SizeInPixels): Location {
        val (screenWidth , screenHeight) = DisplayUtils.getScreenSize(isIgnoreNavigationBars = true, isIgnoreStatusBars = true)
        // 扣除視窗的寬高，讓視窗範圍在螢幕內
        val boundaryX = screenWidth - mainWindowSize.width
        val boundaryY = screenHeight - mainWindowSize.height

        val correctedCoordinateX = if (coordinateX >= boundaryX) boundaryX else coordinateX
        val correctedCoordinateY = if (coordinateY >= boundaryY) boundaryY else coordinateY
        return Location(correctedCoordinateX, correctedCoordinateY)
    }

}