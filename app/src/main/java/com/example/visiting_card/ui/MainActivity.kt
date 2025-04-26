package com.example.visiting_card.ui

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import com.example.visiting_card.R
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MainActivity : ComponentActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Найти BottomSheet по ID
        val bottomSheet = findViewById<View>(R.id.bottomSheet)

        if (bottomSheet != null) {
            // Настроить поведение BottomSheet
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

            // Состояние по умолчанию - свернуто
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            // Высота в свернутом состоянии
            bottomSheetBehavior.peekHeight = 100

            // Чтобы нельзя было полностью скрыть
            bottomSheetBehavior.isHideable = false
        }

        // Добавим обработчик клика для переключения состояния BottomSheet
        val businessCard = findViewById<View>(R.id.business_card) // если у вас есть такая карточка
        businessCard?.setOnClickListener {
            // Переключаем состояние BottomSheet
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }
}
