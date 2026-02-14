<template>
  <div class="flex flex-col gap-6 relative z-20">
    <Card>
      <CardHeader>
        <CardTitle class="text-2xl">{{ $t('questionnaire.title') }}</CardTitle>
        <CardDescription>{{ $t('questionnaire.subtitle') }}</CardDescription>
      </CardHeader>
      <CardContent>
        <div v-if="loading" class="flex items-center justify-center py-8">
          <div class="text-white/60">{{ $t('common.loading') }}</div>
        </div>

        <div v-else-if="!questionnaireEnabled" class="text-center py-8">
          <p class="text-white/60">{{ $t('questionnaire.not_enabled') }}</p>
          <Button class="mt-4" @click="$emit('skip')">{{ $t('questionnaire.continue') }}</Button>
        </div>

        <div v-else-if="submitted" class="text-center py-8">
          <div :class="result.passed ? 'text-green-400' : 'text-red-400'" class="text-xl font-bold mb-4">
            {{ result.passed ? $t('questionnaire.passed') : $t('questionnaire.failed') }}
          </div>
          <p class="text-white/60 mb-2">
            {{ $t('questionnaire.your_score') }}: {{ result.score }} / {{ result.pass_score }}
          </p>
          <Button class="mt-4" @click="resetForm">
            {{ $t('questionnaire.retry') }}
          </Button>
        </div>

        <form v-else @submit.prevent="handleSubmit" class="flex flex-col gap-6">
          <div v-for="(question, qIndex) in questions" :key="question.id" class="question-item">
            <div class="mb-3">
              <span class="text-white/40 text-sm mr-2">Q{{ qIndex + 1 }}.</span>
              <span class="text-white font-medium">{{ question.question }}</span>
              <span v-if="question.required" class="text-red-400 ml-1">*</span>
              <span v-if="question.type === 'multiple_choice'" class="text-white/40 text-sm ml-2">
                ({{ $t('questionnaire.multiple_choice') }})
              </span>
            </div>

            <div v-if="question.type === 'single_choice' || question.type === 'multiple_choice'" class="flex flex-col gap-2 pl-6">
              <label
                v-for="option in question.options"
                :key="option.id"
                class="flex items-center gap-3 p-3 rounded-lg border border-white/10 bg-white/5 hover:bg-white/10 cursor-pointer transition-colors"
                :class="{ 'border-blue-500/50 bg-blue-500/10': isSelected(question.id, option.id) }"
              >
                <input
                  v-if="question.type === 'single_choice'"
                  type="radio"
                  :name="`question_${question.id}`"
                  :value="option.id"
                  v-model="answers[question.id].selectedOptionIds[0]"
                  class="w-4 h-4 text-blue-500"
                />
                <input
                  v-else
                  type="checkbox"
                  :value="option.id"
                  v-model="answers[question.id].selectedOptionIds"
                  class="w-4 h-4 text-blue-500"
                />
                <span class="text-white/80">{{ option.text }}</span>
              </label>
            </div>

            <div v-else-if="question.type === 'text'" class="pl-6">
              <textarea
                v-if="question.input?.multiline"
                v-model="answers[question.id].textAnswer"
                :placeholder="question.input?.placeholder || $t('questionnaire.text_placeholder')"
                :maxlength="question.input?.max_length || undefined"
                rows="4"
                class="w-full rounded-lg border border-white/10 bg-white/5 p-3 text-white placeholder:text-white/40"
              />
              <input
                v-else
                type="text"
                v-model="answers[question.id].textAnswer"
                :placeholder="question.input?.placeholder || $t('questionnaire.text_placeholder')"
                :maxlength="question.input?.max_length || undefined"
                class="w-full rounded-lg border border-white/10 bg-white/5 p-3 text-white placeholder:text-white/40"
              />
              <div class="text-xs text-white/50 mt-2">
                {{ $t('questionnaire.length_hint', {
                  min: question.input?.min_length || 0,
                  max: question.input?.max_length || 0
                }) }}
              </div>
            </div>

            <div v-else class="pl-6 text-yellow-300 text-sm">
              {{ $t('questionnaire.unsupported_type') }}: {{ question.type }}
            </div>
          </div>

          <div class="flex gap-4 mt-4">
            <Button type="button" variant="outline" @click="$emit('back')" class="flex-1">
              {{ $t('common.back') }}
            </Button>
            <Button type="submit" :disabled="submitting || !isFormValid" class="flex-1">
              <span v-if="submitting">{{ $t('common.loading') }}</span>
              <span v-else>{{ $t('questionnaire.submit') }}</span>
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useNotification } from '@/composables/useNotification'

const { t, locale } = useI18n()
import Card from './ui/Card.vue'
import CardHeader from './ui/CardHeader.vue'
import CardTitle from './ui/CardTitle.vue'
import CardDescription from './ui/CardDescription.vue'
import CardContent from './ui/CardContent.vue'
import Button from './ui/Button.vue'

interface QuestionnaireAnswer {
  type: string
  selectedOptionIds: number[]
  textAnswer: string
}

const emit = defineEmits(['passed', 'back', 'skip'])
const notification = useNotification()

const loading = ref(true)
const submitting = ref(false)
const submitted = ref(false)
const questionnaireEnabled = ref(false)
const questions = ref<any[]>([])
const answers = reactive<Record<number, QuestionnaireAnswer>>({})
const result = ref<{
  passed: boolean
  score: number
  pass_score: number
  answers: Record<string, QuestionnaireAnswer>
  token: string
  submitted_at: number
  expires_at: number
}>({
  passed: false,
  score: 0,
  pass_score: 60,
  answers: {},
  token: '',
  submitted_at: 0,
  expires_at: 0
})

const isFormValid = computed(() => {
  return questions.value.every(q => {
    const answer = answers[q.id]
    if (!answer) return !q.required
    if (q.type === 'multiple_choice' || q.type === 'single_choice') {
      const min = q.input?.min_selections ?? (q.required ? 1 : 0)
      const max = q.input?.max_selections ?? Number.MAX_SAFE_INTEGER
      return answer.selectedOptionIds.length >= min && answer.selectedOptionIds.length <= max
    }
    if (q.type === 'text') {
      const text = (answer.textAnswer || '').trim()
      const min = q.input?.min_length ?? 0
      const max = q.input?.max_length ?? Number.MAX_SAFE_INTEGER
      if (!q.required && text.length === 0) return true
      return text.length >= min && text.length <= max
    }
    return false
  })
})

const isSelected = (questionId: number, optionId: number) => {
  return answers[questionId]?.selectedOptionIds?.includes(optionId) || false
}

const initAnswer = (q: any): QuestionnaireAnswer => ({
  type: q.type,
  selectedOptionIds: [],
  textAnswer: ''
})

onMounted(async () => {
  await loadQuestionnaire()
})

const loadQuestionnaire = async () => {
  loading.value = true
  try {
    const response = await fetch(`/api/questionnaire?language=${locale.value}`)
    const data = await response.json()

    if (data.success && data.data) {
      questionnaireEnabled.value = data.data.enabled
      questions.value = data.data.questions || []
      questions.value.forEach(q => {
        answers[q.id] = initAnswer(q)
      })
    }
  } catch (error) {
    console.error('Failed to load questionnaire:', error)
    notification.error(t('common.error'), t('questionnaire.load_error'))
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!isFormValid.value) {
    notification.error(t('common.error'), t('questionnaire.answer_all'))
    return
  }

  submitting.value = true

  try {
    const formattedAnswers: Record<string, QuestionnaireAnswer> = {}
    for (const [questionId, answer] of Object.entries(answers)) {
      formattedAnswers[questionId] = {
        type: answer.type,
        selectedOptionIds: Array.isArray(answer.selectedOptionIds) ? [...answer.selectedOptionIds] : [],
        textAnswer: answer.textAnswer || ''
      }
    }

    const response = await fetch('/api/submit-questionnaire', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        answers: formattedAnswers,
        language: locale.value
      })
    })

    const data = await response.json()

    if (data.success) {
      const submissionResult = {
        passed: data.passed,
        score: data.score,
        pass_score: data.pass_score,
        answers: formattedAnswers,
        token: data.token || '',
        submitted_at: data.submitted_at || Date.now(),
        expires_at: data.expires_at || Date.now()
      }
      result.value = submissionResult

      if (data.passed) {
        notification.success(t('questionnaire.passed'), data.msg)
        emit('passed', submissionResult)
      } else {
        submitted.value = true
        notification.warning(t('questionnaire.failed'), data.msg)
      }
    } else {
      notification.error(t('common.error'), data.msg || t('questionnaire.submit_error'))
    }
  } catch (error) {
    console.error('Failed to submit questionnaire:', error)
    notification.error(t('common.error'), t('questionnaire.submit_error'))
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  submitted.value = false
  questions.value.forEach(q => {
    answers[q.id] = initAnswer(q)
  })
}
</script>

<style scoped>
.question-item {
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.question-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
</style>
