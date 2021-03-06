package com.example.atividade01

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.example.atividade01.adapter.FileAdapter
import com.example.atividade01.data.FileData
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream

open class MainActivity : AppCompatActivity(), FileAdapter.OnItemClickListener {
    private var baseList = ArrayList<FileData>()
    private var adapter = FileAdapter(baseList, this)

    companion object {
        const val DETAILS_REQUEST_CODE = 1
        const val DETAILS_FILE = "File"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        file_list.adapter = adapter
        file_list.layoutManager = LinearLayoutManager(this)
        file_list.setHasFixedSize(true)

        baseList.addAll(getBaseList())
        adapter.notifyDataSetChanged()
    }

    private fun getBaseList(): ArrayList<FileData> {
        var isInternal = isInternalSelected()
        val list = ArrayList<FileData>()

        if (isInternal) {
            this.fileList().forEach { file ->
                list.add(FileData(name = file, isInternal = true))
            }
        } else {
            this.getExternalFilesDir(null)?.listFiles()?.forEach { file ->
                list.add(FileData(name = file.toString(), isInternal = false))
            }
        }

        return list
    }

    fun filterData(view: View) {
        var isInternal = isInternalSelected()

        baseList.clear()

        if (isInternal) {
            this.fileList().forEach { file ->
                baseList.add(FileData(name = file, isInternal = true))
            }
        } else {
            this.getExternalFilesDir(null)?.listFiles()?.forEach { file ->
                baseList.add(
                    FileData(
                        name = file.toString()
                            .replace(this.getExternalFilesDir(null).toString() + "/", ""),
                        isInternal = false
                    )
                )
            }
        }

        adapter.notifyDataSetChanged()
    }

    fun createFile(view: View) {
        var isInternal = isInternalSelected()


        val fileName = file_name_input.text.toString()
        val fileContents = file_content_input.text.toString()

        file_name_input.text.clear()
        file_content_input.text.clear()

        // Check if the fileName isn't null
        if (fileName.isEmpty()) {
            Toast.makeText(this, "Provide at least the file name", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if will create an internal or external file
        if (isInternal) {

            // Check if will use jetpack or not
            if (!useJetpack.isChecked) {
                this.createInternalFile(fileName, fileContents)
            } else {
                val file = File(filesDir, fileName)

                if (file.exists()) {
                    file.delete()
                }

                createEncryptedFile(file, fileContents)
            }

        } else {

            // Check if will use jetpack or not
            if (!useJetpack.isChecked) {
                this.createExternalFile(fileName, fileContents)
            } else {
                val file = File(getExternalFilesDir(null), fileName)

                if (file.exists()) {
                    file.delete()
                }

                createEncryptedFile(file, fileContents)
            }
        }

        this.filterData(view)
    }

    private fun createEncryptedFile(file: File, content: String) {
        val context: Context = applicationContext
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)

        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().bufferedWriter().use { bufferedWriter ->
            bufferedWriter.write(content)
        }
    }

    private fun createInternalFile(fileName: String, fileContents: String) {
        try {
            this.openFileOutput(fileName, MODE_PRIVATE).use {
                it.write(fileContents.toByteArray())
            }

            Toast.makeText(this, "File created", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createExternalFile(fileName: String, fileContents: String) {
        val file = File(this.getExternalFilesDir(null), fileName)
        val fileOutputStream = FileOutputStream(file)

        try {
            fileOutputStream.use { stream ->
                stream.write(fileContents.toByteArray())
            }

            Toast.makeText(
                this,
                "File created ${this.getExternalFilesDir(null)}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isInternalSelected(): Boolean {
        val checkedbtn = btn_group.checkedRadioButtonId

        if (checkedbtn == external_btn.id) {
            return false
        }

        return true
    }


    override fun onDeleteButtonClick(position: Int) {
        val isInternal = isInternalSelected()
        val file: FileData = baseList[position]

        if (isInternal) {
            this.deleteFile(file.name)
        } else {
            val externalFile = File(this.getExternalFilesDir(null), file.name)
            externalFile.delete()
        }

        baseList.remove(file)
        adapter.notifyDataSetChanged()
    }

    override fun onItemClick(position: Int) {
        val file: FileData = baseList[position]

        val intent = Intent(this@MainActivity, FileDetails::class.java)
        intent.putExtra(DETAILS_FILE, file)

        startActivityForResult(intent, DETAILS_REQUEST_CODE)
    }
}
