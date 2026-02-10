<template>
  <div class="flex min-h-screen w-full items-center justify-center p-4 md:p-6 lg:p-10 bg-[#030303]">
    <!-- Questionnaire step -->
    <QuestionnaireForm
      v-if="showQuestionnaire"
      @passed="onQuestionnairePassed"
      @skip="onQuestionnaireSkip"
      @back="onQuestionnaireBack"
    />
    <!-- Registration step -->
    <RegistrationForm v-else />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import RegistrationForm from '@/components/RegistrationForm.vue'
import QuestionnaireForm from '@/components/QuestionnaireForm.vue'
import { apiService } from '@/services/api'

const showQuestionnaire = ref(false)
const questionnaireEnabled = ref(false)

onMounted(async () => {
  try {
    const config = await apiService.getConfig()
    questionnaireEnabled.value = config.questionnaire?.enabled || false
    showQuestionnaire.value = questionnaireEnabled.value
  } catch (e) {
    console.error('Failed to load config:', e)
  }
})

const onQuestionnairePassed = () => {
  showQuestionnaire.value = false
}

const onQuestionnaireSkip = () => {
  showQuestionnaire.value = false
}

const onQuestionnaireBack = () => {
  window.history.back()
}
</script>
