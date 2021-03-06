package com.descalante.snapshotsgarsande

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.descalante.snapshotsgarsande.databinding.FragmentAddBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.*

class AddFragment : Fragment() {

    private val PATH_SNAPSHOT = "snapshots"
    @SuppressLint("SimpleDateFormat")
    val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")

    private lateinit var mBinding: FragmentAddBinding
    private lateinit var mStorageReference: StorageReference
    private lateinit var mDatabaseReference: DatabaseReference

    private var mPhotoSelectedUri: Uri? = null

    private val  galleryResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            if (it.data == null){
                mBinding.progressBar.visibility = View.INVISIBLE
                Toast.makeText(context, "debe adjuntar una imagen", Toast.LENGTH_SHORT).show()
            } else {
                mPhotoSelectedUri = it.data?.data
                mBinding.imgPhoto.setImageURI(mPhotoSelectedUri)
                mBinding.tilTitle.visibility = View.VISIBLE
                mBinding.tvMessage.text = getString(R.string.post_message_valid_title)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentAddBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.btnPost.setOnClickListener { postSnapshot() }

        mBinding.btnSelect.setOnClickListener { openGallery() }

        mStorageReference = FirebaseStorage.getInstance().reference
        mDatabaseReference = FirebaseDatabase.getInstance().reference.child(PATH_SNAPSHOT)
    }

    private fun openGallery() {
        val images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val intent = Intent(Intent.ACTION_PICK, images)
        galleryResult.launch(intent)
    }

    private fun postSnapshot() {
        val currentDate = sdf.format(Date())
        mBinding.progressBar.visibility = View.VISIBLE
        val key = mDatabaseReference.push().key!!
        val storageReference = mStorageReference.child(PATH_SNAPSHOT)
                .child(FirebaseAuth.getInstance().currentUser!!.uid).child(key)
        if (mPhotoSelectedUri != null) {
            storageReference.putFile(mPhotoSelectedUri!!)
                    .addOnProgressListener {
                        val progress = (100 * it.bytesTransferred/it.totalByteCount).toDouble()
                        mBinding.progressBar.progress = progress.toInt()
                        mBinding.tvMessage.text = "$progress%"
                    }
                    .addOnCompleteListener{
                        mBinding.progressBar.visibility = View.INVISIBLE
                    }
                    .addOnSuccessListener {
                        Snackbar.make(mBinding.root, "Instant??nea publicada.",
                                Snackbar.LENGTH_SHORT).show()
                        it.storage.downloadUrl.addOnSuccessListener {
                            saveSnapshot( key, it.toString(), mBinding.etTitle.text.toString().trim(), currentDate, FirebaseAuth.getInstance().currentUser?.email.toString())
                            mBinding.tilTitle.visibility = View.GONE
                            mBinding.tvMessage.text = getString(R.string.post_message_title)
                            mBinding.imgPhoto.setImageDrawable(null)
                        }
                    }
                    .addOnFailureListener{
                        Snackbar.make(mBinding.root, "No se pudo subir, intente m??s tarde.",
                                Snackbar.LENGTH_SHORT).show()
                    }
        }
    }

    private fun saveSnapshot(key: String, url: String, title: String, date: String, email: String){
        val snapshot = Snapshot(title = title, photoUrl = url, datePost = date, email = email)
        mDatabaseReference.child(key).setValue(snapshot)
    }

}