import { ref, computed } from 'vue'
import { getVersions } from '@/api/mrp'

export function useVersion(token) {
  const versions = ref([])
  const selectedVersion = ref('')

  const loadVersions = async () => {
    try {
      const data = await getVersions(token.value)
      versions.value = data.data || []
      if (versions.value.length > 0 && !selectedVersion.value) {
        selectedVersion.value = versions.value[0]
      }
    } catch (err) {
      console.error('Load versions error:', err)
    }
  }

  const selectVersion = (version) => {
    selectedVersion.value = version
  }

  const hasVersions = computed(() => versions.value.length > 0)

  return {
    versions,
    selectedVersion,
    loadVersions,
    selectVersion,
    hasVersions
  }
}
