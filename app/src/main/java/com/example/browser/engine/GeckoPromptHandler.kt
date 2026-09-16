package com.example.browser.engine

import android.content.Context
import com.example.viewmodel.BrowserViewModel
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

/**
 * Manejador de diálogos web interactivos (PromptDelegate) para Mozilla GeckoView.
 * 
 * Intercepta los diálogos emitidos por scripts web (JavaScript alert, confirm, prompt,
 * autenticación HTTP y selección de archivos en formularios HTML), convirtiéndolos en
 * eventos para ser presentados mediante diálogos nativos y accesibles de Jetpack Compose.
 */
class GeckoPromptHandler(
    private val context: Context,
    private val viewModel: BrowserViewModel
) : GeckoSession.PromptDelegate {

    override fun onAlertPrompt(
        session: GeckoSession,
        prompt: GeckoSession.PromptDelegate.AlertPrompt
    ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
        val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
        viewModel.postWebPrompt(
            WebPromptRequest.Alert(
                title = prompt.title ?: "Mensaje de la página",
                message = prompt.message ?: "",
                onConfirm = {
                    result.complete(prompt.dismiss())
                    viewModel.dismissWebPrompt()
                }
            )
        )
        return result
    }

    override fun onButtonPrompt(
        session: GeckoSession,
        prompt: GeckoSession.PromptDelegate.ButtonPrompt
    ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
        val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
        viewModel.postWebPrompt(
            WebPromptRequest.Confirm(
                title = prompt.title ?: "Confirmación",
                message = prompt.message ?: "",
                onConfirm = {
                    result.complete(prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE))
                    viewModel.dismissWebPrompt()
                },
                onDismiss = {
                    result.complete(prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE))
                    viewModel.dismissWebPrompt()
                }
            )
        )
        return result
    }

    override fun onTextPrompt(
        session: GeckoSession,
        prompt: GeckoSession.PromptDelegate.TextPrompt
    ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
        val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
        viewModel.postWebPrompt(
            WebPromptRequest.Prompt(
                title = prompt.title ?: "Solicitud de información",
                message = prompt.message ?: "",
                defaultValue = prompt.defaultValue ?: "",
                onConfirm = { text ->
                    result.complete(prompt.confirm(text))
                    viewModel.dismissWebPrompt()
                },
                onDismiss = {
                    result.complete(prompt.dismiss())
                    viewModel.dismissWebPrompt()
                }
            )
        )
        return result
    }

    override fun onAuthPrompt(
        session: GeckoSession,
        prompt: GeckoSession.PromptDelegate.AuthPrompt
    ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
        val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
        viewModel.postWebPrompt(
            WebPromptRequest.HttpAuth(
                host = prompt.title ?: "Servidor web",
                realm = prompt.message,
                onConfirm = { user, pass ->
                    result.complete(prompt.confirm(user, pass))
                    viewModel.dismissWebPrompt()
                },
                onDismiss = {
                    result.complete(prompt.dismiss())
                    viewModel.dismissWebPrompt()
                }
            )
        )
        return result
    }

    override fun onFilePrompt(
        session: GeckoSession,
        prompt: GeckoSession.PromptDelegate.FilePrompt
    ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
        val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
        val isMultiple = prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE
        val mimeTypesList = prompt.mimeTypes?.toList() ?: listOf("*/*")

        viewModel.postWebPrompt(
            WebPromptRequest.FileChooser(
                title = prompt.title ?: "Seleccionar archivo",
                isMultiple = isMultiple,
                mimeTypes = mimeTypesList,
                onFilesSelected = { uris ->
                    if (uris.isNotEmpty()) {
                        if (isMultiple) {
                            result.complete(prompt.confirm(context, uris.toTypedArray()))
                        } else {
                            result.complete(prompt.confirm(context, uris.first()))
                        }
                    } else {
                        result.complete(prompt.dismiss())
                    }
                    viewModel.dismissWebPrompt()
                },
                onDismiss = {
                    result.complete(prompt.dismiss())
                    viewModel.dismissWebPrompt()
                }
            )
        )
        return result
    }
}
