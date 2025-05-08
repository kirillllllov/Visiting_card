package com.example.visiting_card.ui

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class QrCodeDialog(private val vCardData: String) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val imageView = ImageView(requireContext())

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(vCardData, BarcodeFormat.QR_CODE, 600, 600)
        val bmp = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565)
        for (x in 0 until 600) {
            for (y in 0 until 600) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        imageView.setImageBitmap(bmp)

        return AlertDialog.Builder(requireContext())
            .setTitle("QR-код контакта")
            .setView(imageView)
            .setPositiveButton("Закрыть", null)
            .create()
    }
}
