package com.example.iambeta.storage

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.iambeta.R
import com.example.iambeta.mainPage.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_upload.*
import java.net.URLConnection
import java.util.*

class UploadActivity : AppCompatActivity() {

    var selected: Uri? = null
    var mAuth : FirebaseAuth? = null
    var isImage : Boolean? = true
    var mAuthListenter : FirebaseAuth.AuthStateListener? = null
    var firebaseDatabase : FirebaseDatabase? = null
    var myRef : DatabaseReference? = null
    var mStorageRef : StorageReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        mAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        myRef = firebaseDatabase!!.reference
        mStorageRef = FirebaseStorage.getInstance().reference
    }

    fun upload(view: View) {
        // replace with  + combine with a uuid so uid/uuid
        val userId = FirebaseAuth.getInstance().getCurrentUser()!!.getUid();
        val uuid = UUID.randomUUID()
        var imageName = "images/$uuid.jpg"

        if (isImage == false) {
            imageName = "videos/$uuid.3gp"
        }

        val storageReference = mStorageRef!!.child(imageName)
        storageReference.putFile(selected!!).addOnSuccessListener { taskSnapshot ->

            val newReference=FirebaseStorage.getInstance().getReference(imageName)
            newReference.downloadUrl.addOnSuccessListener { uri ->
                val downloadURL=uri.toString()

                val user=mAuth!!.currentUser
                val userEmail=user!!.email.toString()
                val userComment=commentText.text.toString()

                val uuid = UUID.randomUUID()
                val uuidString = uuid.toString()
                val userIdString = userId.toString()
                myRef!!.child("Posts").child(userIdString).child(uuidString).child("useremail").setValue(userEmail)
                myRef!!.child("Posts").child(userIdString).child(uuidString).child("comment").setValue(userComment)
                if (isImage == true) {
                    myRef!!.child("Posts").child(userIdString).child(uuidString).child("downloadUrl").setValue(downloadURL)
                } else {
                    myRef!!.child("Posts").child(userIdString).child(uuidString).child("videoDownloadUrl").setValue(downloadURL)
                }

            }

        }.addOnFailureListener { exception ->
            if (exception!=null) {
                Toast.makeText(applicationContext, exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }.addOnCompleteListener{ task ->
            if (task.isComplete) {
                Toast.makeText(applicationContext, "Post Shared", Toast.LENGTH_LONG).show()

                startActivity(Intent(applicationContext, MainActivity::class.java))
                finish()
            }
        }

    }

    fun uploadFile(view: View){
        //create dialog to choose between uploading an image or a video
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Which of the following would you like to upload?")
            .setPositiveButton("Image"){ _, _ -> selectImage() }
            .setNegativeButton("Video"){ _, _ -> selectVideo() }
        val dialog = dialogBuilder.create()
        dialog.show()

        val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        val layoutParams = btnPositive.layoutParams as LinearLayout.LayoutParams
        layoutParams.weight = 10f
        btnPositive.layoutParams = layoutParams
        btnNegative.layoutParams = layoutParams
    }

    private fun selectImage() {
        if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            isImage = true
            startActivityForResult(intent, 2)
        }
    }

    private fun selectVideo() {
        if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            isImage = false
            startActivityForResult(intent, 2)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, 2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            selected = data.data

            try {

                // add functionality to check if image or video and replace the boolean check in the onclick handlers
                if(isImage == false) {
                    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                    val cursor = applicationContext.contentResolver.query(selected!!, filePathColumn, null, null, null)
                    cursor!!.moveToFirst()

                    val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                    val picturePath = cursor.getString(columnIndex)
                    cursor.close()

                    val preview = ThumbnailUtils.createVideoThumbnail(picturePath, MediaStore.Video.Thumbnails.MICRO_KIND)
                    imageView.setImageBitmap(preview)
                } else {
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selected)
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}