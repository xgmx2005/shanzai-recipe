import { http } from './http'
import type { Profile, ProfileRequest, ProfileSummary } from '@/types'

type ApiProfile = Omit<Profile, 'gender'> & {
  gender: string
}

function toUiGender(gender: string | undefined): Profile['gender'] {
  if (gender === 'MALE') return '男'
  if (gender === 'FEMALE') return '女'
  return gender === '男' ? '男' : '女'
}

function toApiGender(gender: ProfileRequest['gender']) {
  return gender === '男' ? 'MALE' : 'FEMALE'
}

function toUiProfile(profile: ApiProfile): Profile {
  return {
    ...profile,
    gender: toUiGender(profile.gender),
    profileCompleted: Boolean(profile.profileCompleted),
  }
}

function toApiRequest(profile: ProfileRequest) {
  return {
    ...profile,
    gender: toApiGender(profile.gender),
  }
}

export function getProfile() {
  return http.get<ApiProfile | null>('/profile').then((res) => (res.data ? toUiProfile(res.data) : null))
}

export function saveProfile(payload: ProfileRequest) {
  return http.put<ApiProfile>('/profile', toApiRequest(payload)).then((res) => toUiProfile(res.data))
}

export function getProfileSummary() {
  return http.get<ProfileSummary>('/profile/summary').then((res) => res.data)
}
