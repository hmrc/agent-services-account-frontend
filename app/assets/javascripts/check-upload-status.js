/**
 * File upload status checker module
 * This module handles file upload validation and status checking in a polling fashion.
 * It periodically checks the upload status by making HTTP requests to a specified endpoint
 * until either a success or failure condition is met, or maximum attempts are reached.
 */
(function(document, window) {
    const fileUploadForm = document.getElementById('fileUploadForm')
    const uploadInput = document.getElementById('fileToUpload')
    const progressIndicator = document.getElementById('file-upload-progress')

    function checkUploadStatus(config, count) {
        const checkUploadStatusMaxAttempts = Number(config['checkUploadStatusMaxAttempts'])
        if(count > checkUploadStatusMaxAttempts) {
            renderFormError("generic")
        } else {
            window.fetch(config['checkUploadStatusUrl'], {
                credentials: 'include',
                mode: 'cors'
            })
                .then(function(response) {
                    const status = response.status
                    if (status === 202) {
                        window.location.href = config.success
                    } else if (status === 409) {
                        renderFormError("virus")
                    } else if (!response.ok) {
                        renderFormError("generic")
                    } else {
                        setTimeout(function () {
                            checkUploadStatus(config, count+1)
                        }, Number(config['checkUploadStatusIntervalMs']))
                    }
                })
                .catch(function(e) {
                    console.error('Failed to reach the file upload path for checking status of upload', e.message)
                    renderFormError("generic")
                })
        }
    }

    function clearDynamicFormErrors() {
        const existingErrorSummary = document.getElementById('error-summary-display')
        const existingErrorMessage = document.getElementById('fileToUpload-error')
        const groupErrorClass = document.querySelector('.govuk-form-group--error')
        if(groupErrorClass) {
            groupErrorClass.classList.remove('govuk-form-group--error')
        }
        if(existingErrorSummary) {
            existingErrorSummary.remove()
        }
        if(existingErrorMessage) {
            existingErrorMessage.remove()
            uploadInput.removeAttribute('aria-describedby')
            uploadInput.classList.remove('govuk-file-upload--error')
        }
    }

    function renderFormError(errorType) {
        progressIndicator.replaceChildren()
        progressIndicator.classList.remove('active')
        const errorPrefix = 'Error: '
        if(document.title.substring(0, 7) !== errorPrefix) {
            document.title = errorPrefix + document.title
        }
        const submitButton = document.getElementById('upload-button')
        const summaryTpl = document.getElementById(errorType + '-error-summary')
        const summaryContent = document.importNode(summaryTpl .content, true)
        const errorTpl = document.getElementById(errorType + '-error-message')
        const errorContent = document.importNode(errorTpl .content, true)
        const mainContent = document.querySelector('#main-content > div > div')
        const formGroup = mainContent.querySelector('form > .govuk-form-group')
        mainContent.prepend(summaryContent)
        const errorSummary = document.querySelector('.govuk-error-summary')
        formGroup.classList.add('govuk-form-group--error')
        formGroup.insertBefore(errorContent, uploadInput)
        uploadInput.setAttribute('aria-describedby', 'file-upload-error')
        uploadInput.classList.add('govuk-file-upload--error')
        errorSummary && errorSummary.focus()
        submitButton.removeAttribute('disabled')
    }

    if(fileUploadForm && uploadInput && progressIndicator) {
        const config = progressIndicator.dataset
        uploadInput.removeAttribute('required')
        fileUploadForm.setAttribute('novalidate', 'novalidate')
        fileUploadForm.addEventListener('submit', function(event) {
            document.getElementById('upload-button').setAttribute('disabled', 'true')
            clearDynamicFormErrors()
            progressIndicator.classList.add("active")
            const files = uploadInput.files
            const accept = uploadInput.getAttribute("accept").split(",")
            event.preventDefault()
            // validate file input
            if(files.length === 0) {
                renderFormError("empty-file")
            } else if(!accept.find(r => r.trim() === files[0].type)) {
                renderFormError("wrong-file-type")
            } else if(files[0].size > config.maxFileSize) {
                renderFormError("file-too-large")
            } else if(files[0].size === 0) {
                renderFormError("file-empty")
            } else {
                window.setTimeout(function(){
                    const liveRegionTpl = document.getElementById('live-region-content')
                    const liveRegionContent = document.importNode(liveRegionTpl .content, true)
                    progressIndicator.prepend(liveRegionContent)
                }, 200)
                window.setTimeout(async function(){
                    const formData = new FormData()
                    const hiddenFields = document.querySelectorAll('input[type="hidden"]')
                    for(let i = 0; i < hiddenFields.length; i++) {
                        formData.append(hiddenFields[i].name, hiddenFields[i].value);
                    }
                    formData.append('file', files[0]);
                    try {
                        const r = await window
                            .fetch(fileUploadForm.getAttribute("action"), {
                                credentials: 'include',
                                method: 'post',
                                body: formData,
                                mode: 'no-cors'
                            })
                        if(r.status > 399) {
                            renderFormError("generic")
                        } else {
                            checkUploadStatus(config, 1)
                        }
                    } catch (error) {
                        console.error("No response from upscan when uploading file", error);
                        renderFormError("generic")
                    }
                }, Number(config['millisecondsBeforePoll']))
            }
        })
    }
})(document, window)
