package com.kitaab.app.feature.post

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kitaab.app.feature.post.steps.BookDetailsStep
import com.kitaab.app.feature.post.steps.ChooseTypeStep
import com.kitaab.app.feature.post.steps.ConditionStep
import com.kitaab.app.feature.post.steps.PhotosStep
import com.kitaab.app.feature.post.steps.PriceLocationStep
import com.kitaab.app.ui.theme.Teal500

private val stepTitles = mapOf(
    PostStep.CHOOSE_TYPE to "Post a book",
    PostStep.BOOK_DETAILS to "Book details",
    PostStep.CONDITION to "Condition",
    PostStep.PHOTOS to "Photos",
    PostStep.PRICE_LOCATION to "Price & location",
)

private val stepNumbers = PostStep.entries.mapIndexed { index, step -> step to index }.toMap()
private val totalSteps = PostStep.entries.size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    onPostSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PostViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PostEvent.PostSuccess -> onPostSuccess()
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Intercept back press — go to previous step or exit post flow
    BackHandler {
        if (viewModel.isOnFirstStep) {
            onNavigateBack()
        } else {
            viewModel.goToPreviousStep()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stepTitles[state.currentStep] ?: "Post a book",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (viewModel.isOnFirstStep) onNavigateBack()
                                else viewModel.goToPreviousStep()
                            },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
                // Step progress bar — hidden on first step (choose type)
                if (state.currentStep != PostStep.CHOOSE_TYPE) {
                    val progress = ((stepNumbers[state.currentStep] ?: 0).toFloat()) /
                            (totalSteps - 1).toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Teal500,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        },
        bottomBar = {
            // Bottom action button — hidden on step 1 (type selection auto-advances)
            if (state.currentStep != PostStep.CHOOSE_TYPE) {
                val isLastStep = state.currentStep == PostStep.PRICE_LOCATION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Button(
                        onClick = {
                            when (state.currentStep) {
                                PostStep.BOOK_DETAILS -> {
                                    if (viewModel.validateBookDetails()) viewModel.goToNextStep()
                                }
                                PostStep.CONDITION -> {
                                    if (viewModel.validateCondition()) viewModel.goToNextStep()
                                }
                                PostStep.PHOTOS -> viewModel.goToNextStep()
                                PostStep.PRICE_LOCATION -> viewModel.submitListing()
                                PostStep.CHOOSE_TYPE -> Unit
                            }
                        },
                        enabled = !state.isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Text(
                                text = if (isLastStep) "Post listing" else "Continue",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                val stepIndex = { step: PostStep -> stepNumbers[step] ?: 0 }
                if (stepIndex(targetState) > stepIndex(initialState)) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "post_step",
        ) { step ->
            when (step) {
                PostStep.CHOOSE_TYPE -> ChooseTypeStep(
                    selectedType = state.listingType,
                    onTypeSelected = viewModel::onListingTypeSelected,
                )
                PostStep.BOOK_DETAILS -> BookDetailsStep(
                    state = state,
                    onTitleChanged = viewModel::onTitleChanged,
                    onAuthorChanged = viewModel::onAuthorChanged,
                    onPublisherChanged = viewModel::onPublisherChanged,
                    onEditionChanged = viewModel::onEditionChanged,
                    onSubjectChanged = viewModel::onSubjectChanged,
                    onIsbnScanned = viewModel::onIsbnScanned,
                    onExamTagToggled = viewModel::onExamTagToggled,
                    onHasSolutionsToggled = viewModel::onHasSolutionsToggled,
                    onHasNotesToggled = viewModel::onHasNotesToggled,
                )
                PostStep.CONDITION -> ConditionStep(
                    selectedCondition = state.condition,
                    conditionError = state.conditionError,
                    onConditionSelected = viewModel::onConditionSelected,
                )
                PostStep.PHOTOS -> PhotosStep(
                    photoUris = state.photoUris,
                    onPhotosSelected = viewModel::onPhotosSelected,
                    onPhotoRemoved = viewModel::onPhotoRemoved,
                )
                PostStep.PRICE_LOCATION -> PriceLocationStep(
                    state = state,
                    onPriceChanged = viewModel::onPriceChanged,
                    onCityChanged = viewModel::onCityChanged,
                    onPincodeChanged = viewModel::onPincodeChanged,
                    onLocalityChanged = viewModel::onLocalityChanged,
                    onFetchLocation = viewModel::fetchCurrentLocation,
                )
            }
        }
    }
}