package io.legado.app.ui.association

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.model.localBook.LocalBook
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.utils.isJsonArray
import io.legado.app.utils.isJsonObject
import io.legado.app.utils.readText
import java.io.File

class FileAssociationViewModel(application: Application) : BaseViewModel(application) {

    val successLiveData = MutableLiveData<Intent>()
    val errorLiveData = MutableLiveData<String>()

    fun dispatchIndent(uri: Uri) {
        execute {
            val url: String
            //如果是普通的url，需要根据返回的内容判断是什么
            if (uri.scheme == "file" || uri.scheme == "content") {
                val content = if (uri.scheme == "file") {
                    val file = File(uri.path.toString())
                    if (file.exists()) {
                        file.readText()
                    } else {
                        null
                    }
                } else {
                    DocumentFile.fromSingleUri(context, uri)?.readText(context)
                }
                var scheme = ""
                if (content != null) {
                    if (content.isJsonObject() || content.isJsonArray()) {
                        //暂时根据文件内容判断属于什么
                        when {
                            content.contains("bookSourceUrl") -> {
                                scheme = "booksource"
                            }
                            content.contains("sourceUrl") -> {
                                scheme = "rsssource"
                            }
                            content.contains("pattern") -> {
                                scheme = "replace"
                            }
                        }
                    }
                    if (TextUtils.isEmpty(scheme)) {
                        val book = if (uri.scheme == "content") {
                            LocalBook.importFile(uri.toString())
                        } else {
                            LocalBook.importFile(uri.path.toString())
                        }
                        val intent = Intent(context, ReadBookActivity::class.java)
                        intent.putExtra("bookUrl", book.bookUrl)
                        successLiveData.postValue(intent)
                        return@execute
                    }
                } else {
                    errorLiveData.postValue("文件不存在")
                    return@execute
                }
                // content模式下，需要传递完整的路径，方便后续解析
                url = if (uri.scheme == "content") {
                    "yuedu://${scheme}/importonline?src=$uri"
                } else {
                    "yuedu://${scheme}/importonline?src=${uri.path}"
                }

            } else if (uri.scheme == "yuedu") {
                url = uri.toString()
            } else {
                url = "yuedu://booksource/importonline?src=${uri.path}"
            }
            val data = Uri.parse(url)
            val newIndent = Intent(Intent.ACTION_VIEW)
            newIndent.data = data
            successLiveData.postValue(newIndent)
            return@execute
        }.onError {
            it.printStackTrace()
            toast(it.localizedMessage)
        }
    }
}